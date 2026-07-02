package com.example.ytdash.domain.usecase

import com.example.ytdash.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoSortTest {

    private fun video(id: String, title: String, publishedAt: String) = Video(
        id = id,
        title = title,
        description = "",
        publishedAt = publishedAt,
        category = "tech",
        thumbnailUrl = "",
        lat = null,
        lng = null,
    )

    private val oldest = video("1", "AAA Oldest Clip", "2020-01-01T00:00:00Z")
    private val middle = video("2", "Middle Clip", "2022-06-15T00:00:00Z")
    private val newest = video("3", "ZZZ Newest Clip", "2026-03-01T10:00:00Z")

    @Test
    fun `date descending puts newest first`() {
        val result = VideoSort.apply(listOf(oldest, newest, middle), SortOrder.DATE_DESC)
        assertEquals(listOf(newest, middle, oldest), result)
    }

    @Test
    fun `date ascending puts oldest first`() {
        val result = VideoSort.apply(listOf(newest, oldest, middle), SortOrder.DATE_ASC)
        assertEquals(listOf(oldest, middle, newest), result)
    }

    @Test
    fun `title ascending is case-insensitive alphabetical`() {
        val a = video("a", "banana", "2020-01-01T00:00:00Z")
        val b = video("b", "Apple", "2020-01-01T00:00:00Z")
        val result = VideoSort.apply(listOf(a, b), SortOrder.TITLE_ASC)
        assertEquals(listOf(b, a), result)
    }

    @Test
    fun `unparseable dates fall back to epoch instead of crashing`() {
        val bad = video("x", "Bad Date", "not-a-date")
        val result = VideoSort.apply(listOf(newest, bad), SortOrder.DATE_DESC)
        assertEquals(listOf(newest, bad), result)
    }
}
