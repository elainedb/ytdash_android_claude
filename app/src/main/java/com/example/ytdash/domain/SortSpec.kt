package com.example.ytdash.domain

import com.example.ytdash.domain.model.Video

/** Pure comparators — no Android dependency, unit-testable in isolation. */
enum class SortSpec {
    /** As loaded (channel/page order, no reordering) — the default until the user picks one. */
    Natural,
    DateNewestFirst,
    DateOldestFirst,
    TitleAToZ,
    ;

    fun apply(videos: List<Video>): List<Video> = when (this) {
        Natural -> videos
        DateNewestFirst -> videos.sortedByDescending { it.publishedAt }
        DateOldestFirst -> videos.sortedBy { it.publishedAt }
        TitleAToZ -> videos.sortedBy { it.title.lowercase() }
    }
}
