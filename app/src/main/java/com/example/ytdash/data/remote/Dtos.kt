package com.example.ytdash.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- search.list (channel uploads, paginated) ---
@Serializable
data class SearchResponse(
    val items: List<SearchItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SearchItem(
    val id: SearchId = SearchId(),
    val snippet: Snippet = Snippet(),
)

@Serializable
data class SearchId(
    @SerialName("videoId") val videoId: String? = null,
)

@Serializable
data class Snippet(
    val title: String = "",
    val description: String = "",
    val publishedAt: String = "",
    val channelTitle: String = "",
    val thumbnails: Thumbnails = Thumbnails(),
)

@Serializable
data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
) {
    val bestUrl: String get() = (medium ?: high ?: default)?.url.orEmpty()
}

@Serializable
data class Thumbnail(val url: String = "")

// --- videos.list (details + recording location) ---
@Serializable
data class VideosResponse(
    val items: List<VideoItem> = emptyList(),
)

@Serializable
data class VideoItem(
    val id: String = "",
    val snippet: Snippet = Snippet(),
    val recordingDetails: RecordingDetails? = null,
)

@Serializable
data class RecordingDetails(
    val location: Location? = null,
)

@Serializable
data class Location(
    val latitude: Double? = null,
    val longitude: Double? = null,
)
