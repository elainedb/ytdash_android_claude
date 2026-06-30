package com.example.ytdash.domain

import com.example.ytdash.data.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoQueryTest {
    private fun v(id: String, title: String, category: String, date: String) =
        Video(id, title, "desc", date, category, null, null, null)

    private val videos = listOf(
        v("1", "Tech Talk One", "tech", "2026-03-01T10:00:00Z"),
        v("4", "Music Session", "music", "2026-04-01T10:00:00Z"),
        v("7", "AAA Oldest Clip", "news", "2025-12-01T10:00:00Z"),
        v("8", "ZZZ Newest Clip", "music", "2026-06-20T10:00:00Z"),
    )

    @Test
    fun sortByDateDescending_putsNewestFirst() {
        val result = VideoQuery.apply(videos, category = null, sort = SortOrder.DATE_DESC)
        assertEquals("ZZZ Newest Clip", result.first().title)
    }

    @Test
    fun sortByDateAscending_putsOldestFirst() {
        val result = VideoQuery.apply(videos, category = null, sort = SortOrder.DATE_ASC)
        assertEquals("AAA Oldest Clip", result.first().title)
    }

    @Test
    fun sortByTitleAscending_isAlphabetical() {
        val result = VideoQuery.apply(videos, category = null, sort = SortOrder.TITLE_ASC)
        assertEquals("AAA Oldest Clip", result.first().title)
        assertEquals("ZZZ Newest Clip", result.last().title)
    }

    @Test
    fun filterByCategory_keepsOnlyThatBucket() {
        val result = VideoQuery.apply(videos, category = "tech", sort = SortOrder.DATE_DESC)
        assertEquals(1, result.size)
        assertEquals("Tech Talk One", result.first().title)
    }

    @Test
    fun filterIsCaseInsensitive() {
        val result = VideoQuery.apply(videos, category = "MUSIC", sort = SortOrder.DATE_DESC)
        assertEquals(2, result.size)
        // Still date-desc within the bucket.
        assertEquals("ZZZ Newest Clip", result.first().title)
    }

    @Test
    fun categories_areDistinctInFirstAppearanceOrder() {
        assertEquals(listOf("tech", "music", "news"), VideoQuery.categories(videos))
    }
}
