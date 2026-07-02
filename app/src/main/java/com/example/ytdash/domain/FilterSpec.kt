package com.example.ytdash.domain

import com.example.ytdash.domain.model.Video

/** Pure predicate — no Android dependency, unit-testable in isolation. */
data class FilterSpec(val category: String? = null) {
    fun apply(videos: List<Video>): List<Video> {
        val cat = category ?: return videos
        return videos.filter { it.category.equals(cat, ignoreCase = true) }
    }
}
