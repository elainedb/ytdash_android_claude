package com.example.ytdash.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin YouTube Data API v3 client (mock- and real-compatible). Base URL + key are supplied at
 * runtime; the client appends `/youtube/v3/<endpoint>` itself (youtube-api.md §base).
 *
 * NOTE: there is no catch-all "all channels" endpoint — callers MUST iterate the configured source
 * channels and merge. This client only knows how to talk to a single channel / a batch of ids.
 */
class YouTubeApi(
    private val baseUrl: String,
    private val apiKey: String?,
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Follow nextPageToken until exhausted so EVERY page of a channel is fetched (AC-COUNT-01). */
    suspend fun searchChannelAllPages(channelId: String): List<SearchItem> = withContext(Dispatchers.IO) {
        val out = ArrayList<SearchItem>()
        var token: String? = null
        var guard = 0
        do {
            val url = endpoint("search").newBuilder()
                .addQueryParameter("channelId", channelId)
                .addQueryParameter("part", "snippet")
                .addQueryParameter("order", "date")
                .addQueryParameter("type", "video")
                .addQueryParameter("maxResults", "50")
                .apply { if (token != null) addQueryParameter("pageToken", token) }
                .build()
            val resp = get<SearchListResponse>(url)
            out.addAll(resp.items)
            token = resp.nextPageToken
            guard++
        } while (!token.isNullOrEmpty() && guard < 100)
        out
    }

    /** channels.list → the channel's uploads playlist id (playlistItems idiom, 1 quota unit). */
    suspend fun channelUploadsPlaylist(channelId: String): String? = withContext(Dispatchers.IO) {
        val url = endpoint("channels").newBuilder()
            .addQueryParameter("id", channelId)
            .addQueryParameter("part", "contentDetails")
            .build()
        get<ChannelListResponse>(url).items.firstOrNull()
            ?.contentDetails?.relatedPlaylists?.uploads
            ?.takeIf { it.isNotBlank() }
    }

    /** playlistItems.list, following nextPageToken to exhaustion (cheap: 1 unit/call). */
    suspend fun playlistItemsAllPages(playlistId: String): List<SearchItem> = withContext(Dispatchers.IO) {
        val out = ArrayList<SearchItem>()
        var token: String? = null
        var guard = 0
        do {
            val url = endpoint("playlistItems").newBuilder()
                .addQueryParameter("playlistId", playlistId)
                .addQueryParameter("part", "snippet")
                .addQueryParameter("maxResults", "50")
                .apply { if (token != null) addQueryParameter("pageToken", token) }
                .build()
            val resp = get<PlaylistItemsResponse>(url)
            resp.items.forEach { out.add(SearchItem(id = null, snippet = it.snippet)) }
            token = resp.nextPageToken
            guard++
        } while (!token.isNullOrEmpty() && guard < 100)
        out
    }

    /** videos.list for details + recordingDetails.location. Batched by <=50 ids. */
    suspend fun videosDetails(ids: List<String>): List<VideoItem> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        val out = ArrayList<VideoItem>()
        ids.chunked(50).forEach { chunk ->
            val url = endpoint("videos").newBuilder()
                .addQueryParameter("id", chunk.joinToString(","))
                .addQueryParameter("part", "snippet,contentDetails,recordingDetails")
                .build()
            out.addAll(get<VideoListResponse>(url).items)
        }
        out
    }

    private fun endpoint(name: String): HttpUrl {
        val root = baseUrl.trimEnd('/')
        val b = "$root/youtube/v3/$name".toHttpUrl().newBuilder()
        if (!apiKey.isNullOrBlank()) b.addQueryParameter("key", apiKey)
        return b.build()
    }

    private inline fun <reified T> get(url: HttpUrl): T {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            val body = resp.body?.string() ?: throw IOException("empty body for $url")
            return json.decodeFromString<T>(body)
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
