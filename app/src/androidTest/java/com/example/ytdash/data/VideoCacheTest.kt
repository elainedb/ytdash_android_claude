package com.example.ytdash.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ytdash.data.local.VideoCache
import com.example.ytdash.data.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Persistence test (constitution §2): the cache round-trips and survives as the source of truth. */
@RunWith(AndroidJUnit4::class)
class VideoCacheTest {
    private lateinit var cache: VideoCache

    @Before
    fun setUp() {
        cache = VideoCache(ApplicationProvider.getApplicationContext())
        cache.save(emptyList()) // reset
    }

    @Test
    fun saveThenLoad_roundTripsVideos() {
        val videos = listOf(
            Video("VID1", "Tech Talk One", "desc", "2026-03-01T10:00:00Z", "tech",
                "https://i.ytimg.com/vi/VID1/mqdefault.jpg", 48.8566, 2.3522),
            Video("VID2", "ZZZ Newest Clip", "desc", "2026-06-20T10:00:00Z", "music",
                null, null, null),
        )
        cache.save(videos)

        assertTrue(cache.hasData())
        val loaded = cache.load()
        assertEquals(2, loaded.size)
        assertEquals("Tech Talk One", loaded[0].title)
        assertEquals(48.8566, loaded[0].lat!!, 0.0001)
        assertEquals(null, loaded[1].lat)
    }

    @Test
    fun emptyCache_hasNoData() {
        cache.save(emptyList())
        assertFalse(cache.hasData())
        assertTrue(cache.load().isEmpty())
    }
}
