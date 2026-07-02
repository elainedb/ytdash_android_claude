package com.example.ytdash.domain.usecase

import com.example.ytdash.domain.model.Video

/** Pure filter by category (= source-channel label). No Android imports. */
object VideoFilter {
    fun apply(videos: List<Video>, category: String?): List<Video> =
        if (category.isNullOrBlank()) {
            videos
        } else {
            videos.filter { it.category.equals(category, ignoreCase = true) }
        }
}
