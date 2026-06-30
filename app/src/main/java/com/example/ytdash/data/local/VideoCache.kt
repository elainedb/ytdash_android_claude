package com.example.ytdash.data.local

import android.content.Context
import com.example.ytdash.data.model.Video
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Single source of truth on disk (constitution §1.5). Persists the last good video list as JSON in
 * SharedPreferences so an offline relaunch (a fresh process) can still render cached content
 * (AC-CACHE-01). Replace-on-refresh semantics: each successful network load overwrites the cache.
 */
class VideoCache(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("ytdash_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(Video.serializer())

    fun save(videos: List<Video>) {
        prefs.edit()
            .putString(KEY_VIDEOS, json.encodeToString(serializer, videos))
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun load(): List<Video> {
        val raw = prefs.getString(KEY_VIDEOS, null) ?: return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun hasData(): Boolean = load().isNotEmpty()

    fun savedAt(): Long = prefs.getLong(KEY_SAVED_AT, 0L)

    companion object {
        private const val KEY_VIDEOS = "videos_json"
        private const val KEY_SAVED_AT = "videos_saved_at"
    }
}
