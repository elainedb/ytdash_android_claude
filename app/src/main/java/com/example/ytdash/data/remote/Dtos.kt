package com.example.ytdash.data.remote

import kotlinx.serialization.Serializable

/**
 * DTOs mirroring the YouTube Data API v3 JSON shapes (see spec/youtube-api.md). The mock serves the
 * identical shapes, so the SAME parsing handles mock and real. Unknown keys are ignored by the Json
 * configuration in [YouTubeApi].
 */

@Serializable
data class SearchListResponse(
    val items: List<SearchItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SearchItem(
    val id: SearchId? = null,
    val snippet: Snippet? = null,
)

@Serializable
data class SearchId(val videoId: String? = null)

@Serializable
data class Snippet(
    val publishedAt: String? = null,
    val title: String? = null,
    val description: String? = null,
    val channelTitle: String? = null,
    val thumbnails: Thumbnails? = null,
    val resourceId: ResourceId? = null, // playlistItems idiom
)

@Serializable
data class ResourceId(val videoId: String? = null)

@Serializable
data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
) {
    fun bestUrl(): String? = (medium ?: high ?: default)?.url
}

@Serializable
data class Thumbnail(val url: String? = null)

@Serializable
data class VideoListResponse(
    val items: List<VideoItem> = emptyList(),
)

@Serializable
data class VideoItem(
    val id: String? = null,
    val snippet: Snippet? = null,
    val recordingDetails: RecordingDetails? = null,
)

@Serializable
data class RecordingDetails(val location: GeoLocation? = null)

@Serializable
data class GeoLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
)
