package com.example.ytdash.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin YouTube Data API v3 client (works against the mock or the real host — same signatures).
 * [baseUrl] is the host root; this class appends `/youtube/v3/<endpoint>` itself.
 */
class YouTubeApi(
    private val baseUrl: String,
    private val apiKey: String,
    private val client: OkHttpClient = defaultClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Follows nextPageToken until exhausted — returns EVERY video for the channel, all pages. */
    suspend fun searchAllPages(channelId: String): List<SearchItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<SearchItem>()
        var pageToken: String? = null
        var guard = 0
        do {
            val url = (baseUrl + "/youtube/v3/search").toHttpUrl().newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("channelId", channelId)
                .addQueryParameter("part", "snippet")
                .addQueryParameter("order", "date")
                .addQueryParameter("type", "video")
                .addQueryParameter("maxResults", "50")
                .apply { if (pageToken != null) addQueryParameter("pageToken", pageToken) }
                .build()
            val resp = json.decodeFromString<SearchListResponse>(get(url.toString()))
            out += resp.items
            pageToken = resp.nextPageToken
            guard++
        } while (pageToken != null && guard < 100)
        out
    }

    /** Batched videos.list (<=50 ids/call) — fetches snippet + recordingDetails (location). */
    suspend fun videoDetails(ids: List<String>): List<VideoItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        ids.chunked(50).flatMap { chunk ->
            val url = (baseUrl + "/youtube/v3/videos").toHttpUrl().newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("id", chunk.joinToString(","))
                .addQueryParameter("part", "snippet,contentDetails,recordingDetails")
                .build()
            json.decodeFromString<VideoListResponse>(get(url.toString())).items
        }
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} for $url")
            }
            return response.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    companion object {
        private val defaultClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
