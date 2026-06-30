package com.example.ytdash

import com.example.ytdash.domain.AuthGate
import com.example.ytdash.domain.SortOption
import com.example.ytdash.domain.Video
import com.example.ytdash.domain.VideoFilter
import com.example.ytdash.domain.VideoSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicTest {

    private val whitelist = listOf("elaine.batista1105@gmail.com", "edbpmc@gmail.com")

    private fun video(id: String, title: String, date: String, category: String) =
        Video(id, title, "desc", date, category, "")

    private val sample = listOf(
        video("1", "Tech Talk One", "2026-03-01T10:00:00Z", "cronicas"),
        video("8", "ZZZ Newest Clip", "2026-06-20T10:00:00Z", "bike"),
        video("7", "AAA Oldest Clip", "2025-12-01T10:00:00Z", "mnt"),
    )

    @Test
    fun authGate_acceptsWhitelistedEmail_caseInsensitive() {
        assertTrue(AuthGate.isAuthorized("EDBPMC@gmail.com", whitelist))
        assertTrue(AuthGate.isAuthorized("  edbpmc@gmail.com ", whitelist))
    }

    @Test
    fun authGate_rejectsUnknownOrBlankEmail() {
        assertFalse(AuthGate.isAuthorized("intruder@gmail.com", whitelist))
        assertFalse(AuthGate.isAuthorized(null, whitelist))
        assertFalse(AuthGate.isAuthorized("", whitelist))
    }

    @Test
    fun sort_byDateDescending_putsNewestFirst() {
        val sorted = VideoSort.sort(sample, SortOption.DATE_DESC)
        assertEquals("ZZZ Newest Clip", sorted.first().title)
    }

    @Test
    fun sort_byDateAscending_putsOldestFirst() {
        val sorted = VideoSort.sort(sample, SortOption.DATE_ASC)
        assertEquals("AAA Oldest Clip", sorted.first().title)
    }

    @Test
    fun sort_default_preservesAggregationOrder() {
        val sorted = VideoSort.sort(sample, SortOption.DEFAULT)
        assertEquals(listOf("1", "8", "7"), sorted.map { it.id })
    }

    @Test
    fun filter_keepsOnlyMatchingCategory() {
        val filtered = VideoFilter.filter(sample, "cronicas")
        assertEquals(1, filtered.size)
        assertEquals("Tech Talk One", filtered.first().title)
    }

    @Test
    fun filter_nullLabel_returnsEverything() {
        assertEquals(3, VideoFilter.filter(sample, null).size)
    }

    @Test
    fun filter_labels_areDistinctInFirstAppearanceOrder() {
        assertEquals(listOf("cronicas", "bike", "mnt"), VideoFilter.labels(sample))
    }
}
