package com.example.ytdash.data.repo

import com.example.ytdash.data.remote.ChannelConfigEntry
import com.example.ytdash.data.remote.GeoLocationDto
import com.example.ytdash.data.remote.RecordingDetails
import com.example.ytdash.data.remote.SearchListResponse
import com.example.ytdash.data.remote.SearchResultId
import com.example.ytdash.data.remote.SearchResultItem
import com.example.ytdash.data.remote.Thumbnails
import com.example.ytdash.data.remote.VideoItem
import com.example.ytdash.data.remote.VideoListResponse
import com.example.ytdash.data.remote.VideoSnippet
import com.example.ytdash.data.remote.YouTubeApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in the anti-overfit pagination contract (spec.md, youtube-api.md): follow every channel's
 * `nextPageToken` until exhausted, dedupe by videoId, and enrich with `videos.list` locations. A
 * plain fake `YouTubeApi` — no Room, no Context, no instrumentation needed.
 */
private class FakeYouTubeApi(
    private val pagesByChannel: Map<String, List<List<String>>>, // channelId -> pages of videoIds
    private val locations: Map<String, Pair<Double, Double>> = emptyMap(),
) : YouTubeApi {
    var searchCallCount = 0
        private set

    override suspend fun search(
        channelId: String,
        part: String,
        order: String,
        type: String,
        maxResults: Int,
        pageToken: String?,
    ): SearchListResponse {
        searchCallCount++
        val pages = pagesByChannel[channelId].orEmpty()
        val index = pageToken?.toIntOrNull() ?: 0
        if (index >= pages.size) return SearchListResponse(items = emptyList(), nextPageToken = null)
        val page = pages[index]
        val nextToken = if (index + 1 < pages.size) (index + 1).toString() else null
        return SearchListResponse(
            items = page.map { videoId ->
                SearchResultItem(
                    id = SearchResultId(videoId = videoId),
                    snippet = VideoSnippet(
                        publishedAt = "2026-01-01T00:00:00Z",
                        title = "Title $videoId",
                        description = "Desc $videoId",
                        thumbnails = Thumbnails(),
                    ),
                )
            },
            nextPageToken = nextToken,
        )
    }

    override suspend fun videos(ids: String, part: String): VideoListResponse {
        val idList = ids.split(",")
        return VideoListResponse(
            items = idList.map { id ->
                val loc = locations[id]
                VideoItem(
                    id = id,
                    recordingDetails = loc?.let {
                        RecordingDetails(GeoLocationDto(latitude = it.first, longitude = it.second))
                    },
                )
            },
        )
    }
}

class VideoAggregatorTest {

    @Test
    fun `follows nextPageToken until exhausted for every channel`() = runTest {
        val api = FakeYouTubeApi(
            pagesByChannel = mapOf(
                "chan1" to listOf(listOf("v1", "v2"), listOf("v3", "v4"), listOf("v5")),
                "chan2" to listOf(listOf("v6", "v7")),
            ),
        )
        val aggregator = VideoAggregator(api)
        val channels = listOf(ChannelConfigEntry("chan1", "tech"), ChannelConfigEntry("chan2", "music"))

        val result = aggregator.fetchAll(channels)

        assertEquals(7, result.size)
        assertEquals(setOf("v1", "v2", "v3", "v4", "v5", "v6", "v7"), result.map { it.id }.toSet())
        // 3 pages for chan1 + 1 page for chan2
        assertEquals(4, api.searchCallCount)
    }

    @Test
    fun `dedupes videos seen from multiple channels, first channel wins the category`() = runTest {
        val api = FakeYouTubeApi(
            pagesByChannel = mapOf(
                "chan1" to listOf(listOf("shared", "only1")),
                "chan2" to listOf(listOf("shared", "only2")),
            ),
        )
        val aggregator = VideoAggregator(api)
        val channels = listOf(ChannelConfigEntry("chan1", "tech"), ChannelConfigEntry("chan2", "music"))

        val result = aggregator.fetchAll(channels)

        assertEquals(3, result.size)
        val shared = result.first { it.id == "shared" }
        assertEquals("tech", shared.category)
    }

    @Test
    fun `enriches videos with location from videos-list`() = runTest {
        val api = FakeYouTubeApi(
            pagesByChannel = mapOf("chan1" to listOf(listOf("located", "unlocated"))),
            locations = mapOf("located" to (48.8566 to 2.3522)),
        )
        val aggregator = VideoAggregator(api)

        val result = aggregator.fetchAll(listOf(ChannelConfigEntry("chan1", "tech")))

        val located = result.first { it.id == "located" }
        val unlocated = result.first { it.id == "unlocated" }
        assertEquals(48.8566, located.lat!!, 0.0001)
        assertEquals(2.3522, located.lng!!, 0.0001)
        assertNull(unlocated.lat)
        assertNull(unlocated.lng)
    }

    @Test
    fun `an unconfigured or empty channel yields nothing, not a catch-all`() = runTest {
        val api = FakeYouTubeApi(pagesByChannel = mapOf("chan1" to listOf(listOf("v1"))))
        val aggregator = VideoAggregator(api)

        val result = aggregator.fetchAll(listOf(ChannelConfigEntry("unknown-channel", "tech")))

        assertTrue(result.isEmpty())
    }
}
