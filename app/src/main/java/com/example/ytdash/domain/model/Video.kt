package com.example.ytdash.domain.model

/** Framework-agnostic domain model — no Android, network, or persistence types. */
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String, // ISO-8601 UTC, lexically sortable
    val category: String, // source-channel label, per spec/youtube-api.md
    val thumbnailUrl: String,
    val lat: Double?,
    val lng: Double?,
) {
    val hasLocation: Boolean get() = lat != null && lng != null
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$id"
}

data class ChannelConfig(
    val id: String,
    val label: String,
)
