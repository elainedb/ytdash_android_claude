package com.example.ytdash

import com.example.ytdash.data.cache.VideoCacheCodec
import com.example.ytdash.data.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCacheCodecTest {

    @Test
    fun roundTripsVideosThroughJson() {
        val videos = listOf(
            Video("1", "Tech Talk One", "A talk", "2026-03-01T10:00:00Z", "cronicas", "thumb", 48.8566, 2.3522),
            Video("8", "ZZZ Newest Clip", "newest", "2026-06-20T10:00:00Z", "bike", "thumb", null, null),
        )
        val decoded = VideoCacheCodec.decode(VideoCacheCodec.encode(videos))
        assertEquals(videos, decoded)
        assertEquals(48.8566, decoded[0].lat!!, 0.0)
        assertTrue(decoded[1].lat == null)
    }

    @Test
    fun decodeOfGarbageYieldsEmptyList() {
        assertTrue(VideoCacheCodec.decode("not json").isEmpty())
        assertTrue(VideoCacheCodec.decode("").isEmpty())
    }
}
