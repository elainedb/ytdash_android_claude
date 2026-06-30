package com.example.ytdash.domain

import com.example.ytdash.data.model.Video

enum class SortOrder(val label: String) {
    // Labels are anchored so Maestro's full-string `matches()` resolves the right one:
    // AC-SORT-01 taps text "(?i)date.*(desc|newest)" → must END with the keyword (see
    // cross-framework-setup §D.3).
    DATE_DESC("Date — newest"),
    DATE_ASC("Date — oldest"),
    TITLE_ASC("Title — A to Z"),
    TITLE_DESC("Title — Z to A"),
}

/** Pure domain logic for filtering and sorting the loaded videos. */
object VideoQuery {

    /** Distinct category labels present in the data, in first-appearance order. */
    fun categories(videos: List<Video>): List<String> =
        videos.map { it.category }.filter { it.isNotBlank() }.distinct()

    fun apply(videos: List<Video>, category: String?, sort: SortOrder): List<Video> {
        val filtered = if (category.isNullOrBlank()) {
            videos
        } else {
            videos.filter { it.category.equals(category, ignoreCase = true) }
        }
        return sort(filtered, sort)
    }

    private fun sort(videos: List<Video>, order: SortOrder): List<Video> = when (order) {
        SortOrder.DATE_DESC -> videos.sortedByDescending { it.publishedAt }
        SortOrder.DATE_ASC -> videos.sortedBy { it.publishedAt }
        SortOrder.TITLE_ASC -> videos.sortedBy { it.title.lowercase() }
        SortOrder.TITLE_DESC -> videos.sortedByDescending { it.title.lowercase() }
    }
}
