package com.example.ytdash.domain.usecase

import com.example.ytdash.domain.model.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFilterTest {

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

    private val tech1 = video("1", "tech")
    private val tech2 = video("2", "tech")
    private val music = video("3", "music")

    @Test
    fun `null category returns everything unfiltered`() {
        assertEquals(listOf(tech1, tech2, music), VideoFilter.apply(listOf(tech1, tech2, music), null))
    }

    @Test
    fun `blank category returns everything unfiltered`() {
        assertEquals(listOf(tech1, tech2, music), VideoFilter.apply(listOf(tech1, tech2, music), "  "))
    }

    @Test
    fun `filters to only the matching category`() {
        assertEquals(listOf(tech1, tech2), VideoFilter.apply(listOf(tech1, tech2, music), "tech"))
    }

    @Test
    fun `filter is case-insensitive`() {
        assertEquals(listOf(tech1, tech2), VideoFilter.apply(listOf(tech1, tech2, music), "TECH"))
    }

    @Test
    fun `filter with no matches returns empty list`() {
        assertEquals(emptyList<Video>(), VideoFilter.apply(listOf(tech1, tech2, music), "news"))
    }
}
