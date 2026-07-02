package com.example.ytdash.data.remote

import android.content.Context
import com.example.ytdash.domain.model.ChannelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable

/**
 * Reads the configured source channels from the bundled asset (copied verbatim from
 * `config/channels.json` at build time — see app/build.gradle.kts `copyChannelsConfig`).
 * There is no catch-all endpoint (spec/youtube-api.md); every channel here must be fetched and
 * merged individually.
 */
@Singleton
class ChannelConfigProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class ChannelConfigDto(val id: String, val label: String)

    private var cached: List<ChannelConfig>? = null

    fun loadChannels(): List<ChannelConfig> {
        cached?.let { return it }
        val text = context.assets.open("channels.json").bufferedReader().use { it.readText() }
        val parsed = json.decodeFromString<List<ChannelConfigDto>>(text)
            .map { ChannelConfig(id = it.id, label = it.label) }
        cached = parsed
        return parsed
    }
}
