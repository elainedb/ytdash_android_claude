package com.example.ytdash.data

import android.content.Context
import com.example.ytdash.data.model.SourceChannel
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Loads the configured source channels from the bundled asset (mirrors config/channels.json). */
object ChannelConfig {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(context: Context): List<SourceChannel> = try {
        val text = context.assets.open("channels.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<SourceChannel>>(text)
    } catch (_: Exception) {
        emptyList()
    }
}
