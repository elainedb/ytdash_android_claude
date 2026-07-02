package com.example.ytdash.data.repository

import com.example.ytdash.data.channels.ChannelsConfigProvider
import com.example.ytdash.data.local.VideoDao
import com.example.ytdash.data.mapper.toDomain
import com.example.ytdash.data.mapper.toEntity
import com.example.ytdash.data.remote.YouTubeApiService
import com.example.ytdash.data.remote.dto.SnippetDto
import com.example.ytdash.domain.model.GeoLocation
import com.example.ytdash.domain.model.Video
import com.example.ytdash.domain.repository.VideoRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val api: YouTubeApiService,
    private val dao: VideoDao,
    private val channelsConfigProvider: ChannelsConfigProvider,
) : VideoRepository {

    override fun observeVideos(): Flow<List<Video>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val channels = channelsConfigProvider.channels()
        // There is no catch-all channel query — iterate every configured channel and merge/dedupe
        // by videoId. First-seen snippet wins if the (shouldn't-happen) same id appears twice.
        val snippetsByVideoId = LinkedHashMap<String, SnippetDto>()
        for (channel in channels) {
            var pageToken: String? = null
            do {
                val response = api.search(channelId = channel.id, pageToken = pageToken)
                for (item in response.items) {
                    val videoId = item.id.videoId ?: continue
                    snippetsByVideoId.putIfAbsent(videoId, item.snippet)
                }
                pageToken = response.nextPageToken
            } while (pageToken != null)
        }

        // search.list doesn't carry location; batch videos.list (max 50 ids/call) for it.
        val locationByVideoId = mutableMapOf<String, GeoLocation?>()
        snippetsByVideoId.keys.chunked(50).forEach { chunk ->
            val response = api.videos(ids = chunk.joinToString(","))
            for (item in response.items) {
                val location = item.recordingDetails?.location
                locationByVideoId[item.id] = location?.let { GeoLocation(it.latitude, it.longitude) }
            }
        }

        val videos = snippetsByVideoId.map { (id, snippet) ->
            Video(
                id = id,
                title = snippet.title,
                description = snippet.description,
                publishedAt = parseInstant(snippet.publishedAt),
                category = snippet.channelTitle,
                thumbnailUrl = snippet.thumbnails.medium?.url
                    ?: snippet.thumbnails.high?.url
                    ?: snippet.thumbnails.default?.url
                    ?: "",
                location = locationByVideoId[id],
            )
        }

        dao.replaceAll(videos.map { it.toEntity() })
    }

    private fun parseInstant(iso8601: String): Instant = try {
        Instant.parse(iso8601)
    } catch (e: Exception) {
        Instant.EPOCH
    }
}
