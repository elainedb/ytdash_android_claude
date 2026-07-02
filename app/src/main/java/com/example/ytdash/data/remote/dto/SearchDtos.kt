package com.example.ytdash.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchListResponse(
    val kind: String? = null,
    val nextPageToken: String? = null,
    val items: List<SearchResultItem> = emptyList(),
)

@Serializable
data class SearchResultItem(
    val id: SearchResultId? = null,
    val snippet: VideoSnippet? = null,
)

@Serializable
data class SearchResultId(
    val kind: String? = null,
    val videoId: String? = null,
)

@Serializable
data class VideoSnippet(
    val publishedAt: String? = null,
    val channelId: String? = null,
    val title: String = "",
    val description: String = "",
    val channelTitle: String? = null,
    val categoryId: String? = null,
    val thumbnails: Thumbnails? = null,
)

@Serializable
data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
)

@Serializable
data class Thumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)
