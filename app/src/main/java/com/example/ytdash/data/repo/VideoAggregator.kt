package com.example.ytdash.data.repo

import com.example.ytdash.data.remote.ChannelConfigEntry
import com.example.ytdash.data.remote.VideoSnippet
import com.example.ytdash.data.remote.YouTubeApi
import javax.inject.Inject

/** Flat, Room/Context-free result of aggregating every configured channel — directly testable. */
data class AggregatedVideo(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val category: String,
    val thumbnailUrl: String,
    val lat: Double?,
    val lng: Double?,
)

/**
 * The core anti-overfit logic (spec.md §Data, youtube-api.md): there is NO catch-all/"all
 * channels" endpoint, so this iterates every configured channel, follows `search.list`'s
 * `nextPageToken` until exhausted for EACH channel, unions + dedupes by videoId (first-seen
 * channel's label wins — "category" = source-channel label, not YouTube's categoryId), then
 * batches `videos.list` (≤50 ids/call) to backfill `recordingDetails.location`.
 *
 * Depends only on [YouTubeApi] (an interface, no Android types) so it is unit-testable with a
 * fake implementation and no Room/Context/instrumentation — see PaginationTest.
 */
class VideoAggregator @Inject constructor(
    private val api: YouTubeApi,
) {
    suspend fun fetchAll(channels: List<ChannelConfigEntry>): List<AggregatedVideo> {
        val merged = LinkedHashMap<String, Pair<VideoSnippet, String>>()

        for (channel in channels) {
            var pageToken: String? = null
            do {
                val response = api.search(channelId = channel.id, pageToken = pageToken)
                for (item in response.items) {
                    val videoId = item.id.videoId ?: continue
                    merged.putIfAbsent(videoId, item.snippet to channel.label)
                }
                pageToken = response.nextPageToken
            } while (pageToken != null)
        }

        val locations = HashMap<String, Pair<Double, Double>>()
        merged.keys.chunked(50).forEach { chunk ->
            val response = api.videos(ids = chunk.joinToString(","))
            for (videoItem in response.items) {
                val loc = videoItem.recordingDetails?.location
                val lat = loc?.latitude
                val lng = loc?.longitude
                if (lat != null && lng != null) {
                    locations[videoItem.id] = lat to lng
                }
            }
        }

        return merged.map { (id, snippetAndLabel) ->
            val (snippet, label) = snippetAndLabel
            val loc = locations[id]
            AggregatedVideo(
                id = id,
                title = snippet.title,
                description = snippet.description,
                publishedAt = snippet.publishedAt,
                category = label,
                thumbnailUrl = snippet.thumbnails?.medium?.url
                    ?: snippet.thumbnails?.default?.url
                    ?: "",
                lat = loc?.first,
                lng = loc?.second,
            )
        }
    }
}
