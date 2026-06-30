package com.example.ytdash.data.remote

import com.example.ytdash.config.Channel
import com.example.ytdash.domain.Video
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Aggregates videos across the configured source channels. There is NO catch-all endpoint, so we
 * iterate every channel, follow pagination to exhaustion, and dedupe by videoId (preserving the
 * channel/page order). The two phases are split so the UI can render the list right after [fetchSearch]
 * and add map locations from [enrich] a moment later.
 */
class RemoteDataSource(
    private val api: YouTubeApi,
    private val apiKey: String,
) {
    private companion object {
        const val MAX_PAGES_PER_CHANNEL = 50 // safety bound against a misbehaving nextPageToken
    }

    /** Search every channel (channels in parallel, pages sequential), merged + deduped in order. */
    suspend fun fetchSearch(channels: List<Channel>): List<Video> = coroutineScope {
        val perChannel = channels.map { channel ->
            async { fetchChannel(channel) }
        }.awaitAll()

        val ordered = LinkedHashMap<String, Video>()
        perChannel.forEach { videos ->
            videos.forEach { v -> ordered.putIfAbsent(v.id, v) }
        }
        ordered.values.toList()
    }

    private suspend fun fetchChannel(channel: Channel): List<Video> {
        val result = ArrayList<Video>()
        var pageToken: String? = null
        var pages = 0
        do {
            val resp = api.search(key = apiKey, channelId = channel.id, pageToken = pageToken)
            for (item in resp.items) {
                val videoId = item.id.videoId ?: continue
                result.add(item.snippet.toVideo(videoId, channel.label))
            }
            pageToken = resp.nextPageToken
            pages++
        } while (!pageToken.isNullOrBlank() && pages < MAX_PAGES_PER_CHANNEL)
        return result
    }

    /** Attach recording locations from videos.list (only some videos have one). */
    suspend fun enrich(videos: List<Video>): List<Video> = coroutineScope {
        if (videos.isEmpty()) return@coroutineScope videos
        val byId = LinkedHashMap<String, Video>().apply { videos.forEach { put(it.id, it) } }

        val responses = byId.keys.toList().chunked(50).map { batch ->
            async { api.videos(key = apiKey, ids = batch.joinToString(",")) }
        }.awaitAll()

        responses.forEach { resp ->
            for (item in resp.items) {
                val loc = item.recordingDetails?.location ?: continue
                val lat = loc.latitude ?: continue
                val lng = loc.longitude ?: continue
                byId[item.id]?.let { byId[item.id] = it.copy(lat = lat, lng = lng) }
            }
        }
        byId.values.toList()
    }

    private fun Snippet.toVideo(id: String, label: String): Video = Video(
        id = id,
        title = title,
        description = description,
        publishedAt = publishedAt,
        category = label,
        thumbnailUrl = thumbnails.bestUrl,
    )
}
