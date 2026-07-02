package com.example.ytdash.domain.usecase

import com.example.ytdash.domain.model.Video
import java.time.Instant

enum class SortOrder {
    DATE_DESC,
    DATE_ASC,
    TITLE_ASC,
}

/** Pure sort — no Android imports, directly unit-testable. */
object VideoSort {
    fun apply(videos: List<Video>, order: SortOrder): List<Video> = when (order) {
        SortOrder.DATE_DESC -> videos.sortedByDescending { parseInstant(it.publishedAt) }
        SortOrder.DATE_ASC -> videos.sortedBy { parseInstant(it.publishedAt) }
        SortOrder.TITLE_ASC -> videos.sortedBy { it.title.lowercase() }
    }

    private fun parseInstant(value: String): Instant = try {
        Instant.parse(value)
    } catch (e: Exception) {
        Instant.EPOCH
    }
}
