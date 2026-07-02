package com.example.ytdash.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VideoListResponseDto(
    val items: List<VideoDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class VideoDto(
    val id: String,
    val snippet: SnippetDto,
    val recordingDetails: RecordingDetailsDto? = null,
)

@Serializable
data class RecordingDetailsDto(
    val location: LocationDto? = null,
)

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
)
