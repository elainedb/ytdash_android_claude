package com.example.ytdash.domain

/** Authorization rule: a signed-in email must be on the whitelist (case-insensitive). */
object AuthGate {
    fun isAuthorized(email: String?, authorized: List<String>): Boolean {
        val e = email?.trim()?.lowercase().orEmpty()
        if (e.isEmpty()) return false
        return authorized.any { it.trim().lowercase() == e }
    }
}

enum class SortOption(val label: String) {
    // Default keeps the API aggregation order (first channel's first video stays at index 0).
    DEFAULT("Default order"),
    // Labels END with the keyword the harness matches, e.g. "(?i)date.*(desc|newest)".
    DATE_DESC("Date — newest"),
    DATE_ASC("Date — oldest"),
    TITLE_ASC("Title — A to Z"),
    TITLE_DESC("Title — Z to A"),
}

object VideoSort {
    fun sort(videos: List<Video>, option: SortOption): List<Video> = when (option) {
        SortOption.DEFAULT -> videos
        SortOption.DATE_DESC -> videos.sortedByDescending { it.publishedAt }
        SortOption.DATE_ASC -> videos.sortedBy { it.publishedAt }
        SortOption.TITLE_ASC -> videos.sortedBy { it.title.lowercase() }
        SortOption.TITLE_DESC -> videos.sortedByDescending { it.title.lowercase() }
    }
}

object VideoFilter {
    /** Distinct category labels in first-appearance (loaded) order. */
    fun labels(videos: List<Video>): List<String> =
        videos.map { it.category }.filter { it.isNotBlank() }.distinct()

    fun filter(videos: List<Video>, label: String?): List<Video> =
        if (label.isNullOrBlank()) videos
        else videos.filter { it.category.equals(label, ignoreCase = true) }
}
