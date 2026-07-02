package com.example.ytdash.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VideoListResponse(
    val kind: String? = null,
    val nextPageToken: String? = null,
    val items: List<VideoDetailItem> = emptyList(),
)

@Serializable
data class VideoDetailItem(
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
    val location: GeoLocation? = null,
)

@Serializable
data class GeoLocation(
    val latitude: Double? = null,
    val longitude: Double? = null,
)
