package com.example.ytdash.domain

import com.example.ytdash.data.model.Video

enum class SortMode(val label: String) {
    // Labels are chosen so Maestro's full-string `text:` regexes match:
    //   date desc -> "(?i)date.*(desc|newest)"  ; must start with "date", end with the keyword.
    DATE_DESC("Date — newest"),
    DATE_ASC("Date — oldest"),
    TITLE_ASC("Title — A to Z"),
    TITLE_DESC("Title — Z to A"),
}

object VideoOps {

    /** Distinct category labels present in the data, in first-appearance order. */
    fun categories(videos: List<Video>): List<String> =
        videos.map { it.category }.filter { it.isNotBlank() }.distinct()

    /** Keep only the given category; null/blank == no filter. */
    fun filterByCategory(videos: List<Video>, category: String?): List<Video> {
        if (category.isNullOrBlank()) return videos
        return videos.filter { it.category.equals(category, ignoreCase = true) }
    }

    fun sort(videos: List<Video>, mode: SortMode): List<Video> = when (mode) {
        // publishedAt is ISO-8601 → lexicographic compare == chronological.
        SortMode.DATE_DESC -> videos.sortedByDescending { it.publishedAt }
        SortMode.DATE_ASC -> videos.sortedBy { it.publishedAt }
        SortMode.TITLE_ASC -> videos.sortedBy { it.title.lowercase() }
        SortMode.TITLE_DESC -> videos.sortedByDescending { it.title.lowercase() }
    }

    /** Apply optional filter then optional sort. Null sort keeps insertion (aggregation) order. */
    fun apply(videos: List<Video>, category: String?, sort: SortMode?): List<Video> {
        val filtered = filterByCategory(videos, category)
        return if (sort == null) filtered else sort(filtered, sort)
    }
}
