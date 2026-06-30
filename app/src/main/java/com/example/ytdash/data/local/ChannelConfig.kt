package com.example.ytdash.data.local

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A configured source channel: its YouTube channelId and its user-facing category label. */
@Serializable
data class SourceChannel(
    val id: String,
    val label: String,
)

/**
 * Reads the configured source channels from assets/channels.json (synced from config/channels.json
 * at build time). There is NO catch-all endpoint — the app must iterate THESE channels and merge.
 */
object ChannelConfig {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<SourceChannel> = try {
        val text = context.assets.open("channels.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<SourceChannel>>(text).filter { it.id.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }
}
