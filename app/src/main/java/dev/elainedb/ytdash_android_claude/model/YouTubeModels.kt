package dev.elainedb.ytdash_android_claude.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Search endpoint response models

@Serializable
data class YouTubeSearchResponse(
    @SerialName("items") val items: List<YouTubeVideoItem> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null
)

@Serializable
data class YouTubeVideoItem(
    @SerialName("id") val id: YouTubeVideoId,
    @SerialName("snippet") val snippet: YouTubeVideoSnippet
)

@Serializable
data class YouTubeVideoId(
    @SerialName("videoId") val videoId: String? = null
)

@Serializable
data class YouTubeVideoSnippet(
    @SerialName("title") val title: String = "",
    @SerialName("channelTitle") val channelTitle: String = "",
    @SerialName("channelId") val channelId: String = "",
    @SerialName("publishedAt") val publishedAt: String = "",
    @SerialName("thumbnails") val thumbnails: YouTubeThumbnails? = null,
    @SerialName("description") val description: String = ""
)

@Serializable
data class YouTubeThumbnails(
    @SerialName("high") val high: YouTubeThumbnail? = null,
    @SerialName("medium") val medium: YouTubeThumbnail? = null,
    @SerialName("default") val default_: YouTubeThumbnail? = null
)

@Serializable
data class YouTubeThumbnail(
    @SerialName("url") val url: String = ""
)

// Videos endpoint response models (for details)

@Serializable
data class YouTubeVideosResponse(
    @SerialName("items") val items: List<YouTubeVideoDetails> = emptyList()
)

@Serializable
data class YouTubeVideoDetails(
    @SerialName("id") val id: String = "",
    @SerialName("snippet") val snippet: YouTubeVideoDetailsSnippet? = null,
    @SerialName("recordingDetails") val recordingDetails: YouTubeRecordingDetails? = null
)

@Serializable
data class YouTubeVideoDetailsSnippet(
    @SerialName("tags") val tags: List<String>? = null
)

@Serializable
data class YouTubeRecordingDetails(
    @SerialName("location") val location: YouTubeLocation? = null,
    @SerialName("recordingDate") val recordingDate: String? = null
)

@Serializable
data class YouTubeLocation(
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null
)

// Domain model

data class Video(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val locationCity: String? = null,
    val locationCountry: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val recordingDate: String? = null
)

fun YouTubeVideoItem.toVideo(): Video {
    val thumbnailUrl = snippet.thumbnails?.high?.url
        ?: snippet.thumbnails?.medium?.url
        ?: snippet.thumbnails?.default_?.url
        ?: ""

    return Video(
        id = id.videoId ?: "",
        title = snippet.title,
        channelName = snippet.channelTitle,
        channelId = snippet.channelId,
        publishedAt = snippet.publishedAt,
        thumbnailUrl = thumbnailUrl,
        description = snippet.description
    )
}

fun Video.mergeWithDetails(details: YouTubeVideoDetails): Video {
    return copy(
        tags = details.snippet?.tags ?: tags,
        locationLatitude = details.recordingDetails?.location?.latitude ?: locationLatitude,
        locationLongitude = details.recordingDetails?.location?.longitude ?: locationLongitude,
        recordingDate = details.recordingDetails?.recordingDate ?: recordingDate
    )
}
