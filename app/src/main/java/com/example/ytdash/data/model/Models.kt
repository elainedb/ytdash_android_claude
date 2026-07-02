package com.example.ytdash.data.model

import kotlinx.serialization.Serializable

/** A configured source channel to aggregate (from config/channels.json, bundled as an asset). */
@Serializable
data class SourceChannel(
    val id: String,
    val label: String,
)

/** The app's domain model for a video, independent of the API/DTO shapes. Serializable for cache. */
@Serializable
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String, // ISO-8601 — lexicographic order == chronological order
    val category: String,    // the source channel's label (constitution/youtube-api mapping)
    val thumbnailUrl: String,
    val lat: Double? = null,
    val lng: Double? = null,
) {
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$id"
    val hasLocation: Boolean get() = lat != null && lng != null
}
