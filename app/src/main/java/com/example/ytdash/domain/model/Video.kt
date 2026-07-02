package com.example.ytdash.domain.model

/** Domain video model — decoupled from both the Room entity and the YouTube API DTOs. */
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    /** The source channel's configured label (e.g. "music"/"news"/"tech"), not YouTube's categoryId. */
    val category: String,
    val thumbnailUrl: String,
    val lat: Double?,
    val lng: Double?,
) {
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$id"
    val hasLocation: Boolean get() = lat != null && lng != null
}
