package com.example.ytdash.data.model

import kotlinx.serialization.Serializable

/** Domain model for a video. Persisted to the local cache as JSON (so it is @Serializable). */
@Serializable
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String, // ISO-8601; lexicographically sortable
    val category: String,    // the SOURCE CHANNEL'S label (config), not YouTube's categoryId
    val thumbnailUrl: String?,
    val lat: Double?,
    val lng: Double?,
) {
    val hasLocation: Boolean get() = lat != null && lng != null

    /** The canonical external watch URL for this video. */
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$id"
}
