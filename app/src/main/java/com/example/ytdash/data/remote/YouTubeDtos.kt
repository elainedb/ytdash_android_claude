package com.example.ytdash.data.remote

import kotlinx.serialization.Serializable

// DTO shapes mirror spec/youtube-api.md exactly (both the mock and the real YouTube Data API v3
// return these shapes). `ignoreUnknownKeys = true` (see NetworkModule) lets extra real-API fields
// (kind, etag, pageInfo, ...) pass through unmodeled.

@Serializable
data class SearchListResponse(
    val items: List<SearchResultItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SearchResultItem(
    val id: SearchResultId = SearchResultId(),
    val snippet: VideoSnippet = VideoSnippet(),
)

@Serializable
data class SearchResultId(
    val videoId: String? = null,
)

@Serializable
data class VideoSnippet(
    val publishedAt: String = "",
    val channelId: String = "",
    val title: String = "",
    val description: String = "",
    val channelTitle: String = "",
    val thumbnails: Thumbnails? = null,
)

@Serializable
data class Thumbnails(
    val default: ThumbnailInfo? = null,
    val medium: ThumbnailInfo? = null,
    val high: ThumbnailInfo? = null,
)

@Serializable
data class ThumbnailInfo(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class VideoListResponse(
    val items: List<VideoItem> = emptyList(),
)

@Serializable
data class VideoItem(
    val id: String,
    val snippet: VideoSnippet? = null,
    val contentDetails: ContentDetails? = null,
    val recordingDetails: RecordingDetails? = null,
)

@Serializable
data class ContentDetails(
    val duration: String? = null,
)

@Serializable
data class RecordingDetails(
    val location: GeoLocationDto? = null,
)

@Serializable
data class GeoLocationDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class PlaylistItemListResponse(
    val items: List<PlaylistItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class PlaylistItem(
    val snippet: PlaylistItemSnippet = PlaylistItemSnippet(),
)

@Serializable
data class PlaylistItemSnippet(
    val publishedAt: String = "",
    val title: String = "",
    val description: String = "",
    val channelTitle: String = "",
    val thumbnails: Thumbnails? = null,
    val resourceId: SearchResultId = SearchResultId(),
)
