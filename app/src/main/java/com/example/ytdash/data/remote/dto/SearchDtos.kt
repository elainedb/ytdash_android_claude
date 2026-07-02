package com.example.ytdash.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchListResponseDto(
    val items: List<SearchResultDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SearchResultDto(
    val id: SearchResultIdDto,
    val snippet: SnippetDto,
)

@Serializable
data class SearchResultIdDto(
    val videoId: String? = null,
)

@Serializable
data class SnippetDto(
    val publishedAt: String,
    val title: String,
    val description: String = "",
    val channelTitle: String = "",
    val thumbnails: ThumbnailsDto = ThumbnailsDto(),
)

@Serializable
data class ThumbnailsDto(
    val default: ThumbnailDto? = null,
    val medium: ThumbnailDto? = null,
    val high: ThumbnailDto? = null,
)

@Serializable
data class ThumbnailDto(
    val url: String,
    val width: Int = 0,
    val height: Int = 0,
)
