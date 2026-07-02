package com.example.ytdash

import com.example.ytdash.data.model.Video
import com.example.ytdash.domain.SortMode
import com.example.ytdash.domain.VideoOps
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoOpsTest {

    private fun v(id: String, title: String, category: String, date: String) =
        Video(id, title, "desc", date, category, "thumb")

    private val videos = listOf(
        v("1", "Tech Talk One", "tech", "2026-03-01T10:00:00Z"),
        v("7", "AAA Oldest Clip", "news", "2025-12-01T10:00:00Z"),
        v("8", "ZZZ Newest Clip", "music", "2026-06-20T10:00:00Z"),
        v("3", "Tech Deep Dive", "tech", "2026-01-15T10:00:00Z"),
    )

    @Test
    fun sortDateDescendingPutsNewestFirst() {
        val sorted = VideoOps.sort(videos, SortMode.DATE_DESC)
        assertEquals("ZZZ Newest Clip", sorted.first().title)
    }

    @Test
    fun sortDateAscendingPutsOldestFirst() {
        val sorted = VideoOps.sort(videos, SortMode.DATE_ASC)
        assertEquals("AAA Oldest Clip", sorted.first().title)
    }

    @Test
    fun sortTitleAscending() {
        val sorted = VideoOps.sort(videos, SortMode.TITLE_ASC)
        assertEquals("AAA Oldest Clip", sorted.first().title)
    }

    @Test
    fun filterByCategoryKeepsOnlyThatBucket() {
        val filtered = VideoOps.filterByCategory(videos, "tech")
        assertEquals(2, filtered.size)
        assertEquals(setOf("Tech Talk One", "Tech Deep Dive"), filtered.map { it.title }.toSet())
    }

    @Test
    fun nullFilterKeepsEverything() {
        assertEquals(videos.size, VideoOps.filterByCategory(videos, null).size)
    }

    @Test
    fun categoriesInFirstAppearanceOrder() {
        assertEquals(listOf("tech", "news", "music"), VideoOps.categories(videos))
    }

    @Test
    fun applyFilterThenSort() {
        val result = VideoOps.apply(videos, "tech", SortMode.DATE_DESC)
        assertEquals(listOf("Tech Talk One", "Tech Deep Dive"), result.map { it.title })
    }
}
