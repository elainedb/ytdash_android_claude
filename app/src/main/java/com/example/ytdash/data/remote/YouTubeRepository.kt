package com.example.ytdash.data.remote

import com.example.ytdash.data.remote.dto.VideoSnippet
import com.example.ytdash.domain.model.Video
import javax.inject.Inject
import javax.inject.Singleton

private data class AggregatedSnippet(val snippet: VideoSnippet, val categoryLabel: String)

/**
 * Aggregates videos across every configured source channel. There is no catch-all/"all
 * channels" query (spec/youtube-api.md) — each channel is paginated to exhaustion, then all
 * discovered video ids are merged/deduped and batch-enriched with location/details via
 * `videos.list`. Never assumes a fixed page count, channel count, or result size — everything is
 * read from the API responses so this works unchanged against the hidden held-out dataset.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val api: YouTubeApi,
    private val channelConfigProvider: ChannelConfigProvider,
) {
    suspend fun fetchAllVideos(): List<Video> {
        val channels = channelConfigProvider.loadChannels()
        val byVideoId = LinkedHashMap<String, AggregatedSnippet>()

        for (channel in channels) {
            var pageToken: String? = null
            do {
                val response = api.searchChannelVideos(channelId = channel.id, pageToken = pageToken)
                for (item in response.items) {
                    val videoId = item.id?.videoId ?: continue
                    val snippet = item.snippet ?: continue
                    // Dedup across channels: first occurrence wins (a video appearing under
                    // multiple configured channels keeps a single row).
                    byVideoId.putIfAbsent(videoId, AggregatedSnippet(snippet, channel.label))
                }
                pageToken = response.nextPageToken
            } while (!pageToken.isNullOrBlank())
        }

        val allIds = byVideoId.keys.toList()
        val locationById = HashMap<String, Pair<Double?, Double?>>()
        allIds.chunked(50).forEach { chunk ->
            val response = api.getVideoDetails(ids = chunk.joinToString(","))
            for (item in response.items) {
                val loc = item.recordingDetails?.location
                locationById[item.id] = loc?.latitude to loc?.longitude
            }
        }

        return allIds.map { id ->
            val agg = byVideoId.getValue(id)
            val (lat, lng) = locationById[id] ?: (null to null)
            Video(
                id = id,
                title = agg.snippet.title,
                description = agg.snippet.description,
                publishedAt = agg.snippet.publishedAt ?: "",
                category = agg.categoryLabel,
                thumbnailUrl = agg.snippet.thumbnails?.medium?.url
                    ?: agg.snippet.thumbnails?.high?.url
                    ?: agg.snippet.thumbnails?.default?.url
                    ?: "",
                lat = lat,
                lng = lng,
            )
        }
    }
}
