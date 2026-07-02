package com.example.ytdash.domain

import com.example.ytdash.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class SortSpecTest {

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

    private val videos = listOf(
        video("2", "AAA Oldest Clip", "2026-01-01T00:00:00Z"),
        video("1", "Middle Clip", "2026-02-01T00:00:00Z"),
        video("3", "ZZZ Newest Clip", "2026-03-01T00:00:00Z"),
    )

    @Test
    fun `natural leaves the input order unchanged`() {
        val sorted = SortSpec.Natural.apply(videos)
        assertEquals(videos.map { it.title }, sorted.map { it.title })
    }

    @Test
    fun `date newest first sorts descending by publishedAt`() {
        val sorted = SortSpec.DateNewestFirst.apply(videos)
        assertEquals(listOf("ZZZ Newest Clip", "Middle Clip", "AAA Oldest Clip"), sorted.map { it.title })
    }

    @Test
    fun `date oldest first sorts ascending by publishedAt`() {
        val sorted = SortSpec.DateOldestFirst.apply(videos)
        assertEquals(listOf("AAA Oldest Clip", "Middle Clip", "ZZZ Newest Clip"), sorted.map { it.title })
    }

    @Test
    fun `title a to z sorts alphabetically case-insensitively`() {
        val sorted = SortSpec.TitleAToZ.apply(videos)
        assertEquals(listOf("AAA Oldest Clip", "Middle Clip", "ZZZ Newest Clip"), sorted.map { it.title })
    }
}
