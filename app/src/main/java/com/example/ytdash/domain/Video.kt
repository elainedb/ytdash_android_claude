package com.example.ytdash.domain

/**
 * Domain model for a single video. Framework-neutral; the presentation and data layers both depend
 * on this, never on each other's types.
 */
data class Video(
    val id: String,
    val title: String,
    val description: String,
    /** ISO-8601 UTC timestamp (e.g. 2026-03-01T10:00:00Z). Lexicographically sortable. */
    val publishedAt: String,
    /** The source channel's configured label (e.g. "cronicas"), used for filtering. */
    val category: String,
    val thumbnailUrl: String,
    val lat: Double? = null,
    val lng: Double? = null,
) {
    val youtubeUrl: String get() = "https://www.youtube.com/watch?v=$id"
    val hasLocation: Boolean get() = lat != null && lng != null
}
