package com.example.ytdash.data.cache

import android.content.Context
import com.example.ytdash.data.model.Video
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Pure (Android-free) encode/decode of the cached list — unit-testable on the JVM. */
object VideoCacheCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(videos: List<Video>): String = json.encodeToString(videos)

    fun decode(text: String): List<Video> = try {
        json.decodeFromString<List<Video>>(text)
    } catch (_: Exception) {
        emptyList()
    }
}

/** Local persistence = the single source of truth the UI reads from (constitution §1.5). */
interface VideoCache {
    fun load(): List<Video>
    fun save(videos: List<Video>)
    fun isEmpty(): Boolean
}

class FileVideoCache(context: Context) : VideoCache {
    private val file = File(context.filesDir, "videos_cache.json")

    override fun load(): List<Video> =
        if (file.exists()) VideoCacheCodec.decode(file.readText()) else emptyList()

    override fun save(videos: List<Video>) {
        file.writeText(VideoCacheCodec.encode(videos))
    }

    override fun isEmpty(): Boolean = !file.exists() || file.length() == 0L
}
