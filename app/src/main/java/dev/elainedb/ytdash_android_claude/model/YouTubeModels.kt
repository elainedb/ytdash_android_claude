package dev.elainedb.ytdash_android_claude.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("videoId") val videoId: String
)

@Serializable
data class YouTubeVideoSnippet(
    @SerialName("title") val title: String,
    @SerialName("channelTitle") val channelTitle: String,
    @SerialName("channelId") val channelId: String,
    @SerialName("publishedAt") val publishedAt: String,
    @SerialName("description") val description: String = "",
    @SerialName("thumbnails") val thumbnails: YouTubeThumbnails? = null
)

@Serializable
data class YouTubeThumbnails(
    @SerialName("medium") val medium: YouTubeThumbnail? = null,
    @SerialName("high") val high: YouTubeThumbnail? = null,
    @SerialName("default") val default_: YouTubeThumbnail? = null
)

@Serializable
data class YouTubeThumbnail(
    @SerialName("url") val url: String
)

// Video details models
@Serializable
data class YouTubeVideosResponse(
    @SerialName("items") val items: List<YouTubeVideoDetails> = emptyList()
)

@Serializable
data class YouTubeVideoDetails(
    @SerialName("id") val id: String,
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

fun YouTubeVideoItem.toVideo(): Video {
    return Video(
        id = id.videoId,
        title = snippet.title,
        channelName = snippet.channelTitle,
        channelId = snippet.channelId,
        publishedAt = snippet.publishedAt,
        thumbnailUrl = snippet.thumbnails?.high?.url
            ?: snippet.thumbnails?.medium?.url
            ?: snippet.thumbnails?.default_?.url
            ?: "",
        description = snippet.description
    )
}

fun Video.mergeWithDetails(details: YouTubeVideoDetails): Video {
    return copy(
        tags = details.snippet?.tags ?: emptyList(),
        locationLatitude = details.recordingDetails?.location?.latitude,
        locationLongitude = details.recordingDetails?.location?.longitude,
        recordingDate = details.recordingDetails?.recordingDate
    )
}
