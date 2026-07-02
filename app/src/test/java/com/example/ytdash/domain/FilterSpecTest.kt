package com.example.ytdash.domain

import com.example.ytdash.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterSpecTest {

    private fun video(id: String, category: String) = Video(
        id = id,
        title = "Video $id",
        description = "",
        publishedAt = "2026-01-01T00:00:00Z",
        category = category,
        thumbnailUrl = "",
        lat = null,
        lng = null,
    )

    private val videos = listOf(video("1", "tech"), video("2", "music"), video("3", "tech"))

    @Test
    fun `no category returns all videos`() {
        assertEquals(3, FilterSpec(null).apply(videos).size)
    }

    @Test
    fun `filtering by category keeps only matching videos`() {
        val filtered = FilterSpec("tech").apply(videos)
        assertEquals(listOf("1", "3"), filtered.map { it.id })
    }

    @Test
    fun `filtering is case-insensitive`() {
        val filtered = FilterSpec("TECH").apply(videos)
        assertEquals(2, filtered.size)
    }
}
