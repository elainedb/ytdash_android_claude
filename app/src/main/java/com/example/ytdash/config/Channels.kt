package com.example.ytdash.config

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A configured source channel to aggregate. `label` is its user-facing category. */
@Serializable
data class Channel(val id: String, val label: String)

object ChannelsLoader {
    private val json = Json { ignoreUnknownKeys = true }

    /** Read the configured source channels from the bundled assets/channels.json (data-driven). */
    fun load(context: Context): List<Channel> =
        context.assets.open("channels.json").bufferedReader().use { reader ->
            json.decodeFromString<List<Channel>>(reader.readText())
        }
}
