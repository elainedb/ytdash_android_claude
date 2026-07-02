package com.example.ytdash.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class ChannelConfigEntry(
    val id: String,
    val label: String,
)

/**
 * Reads the configured source channels from `assets/channels.json` at runtime (Gradle-copied from
 * the repo-root `config/channels.json` — see app/build.gradle.kts `copyChannelsConfig`). Never a
 * hardcoded Kotlin list, per spec.md's anti-overfit requirement.
 */
class ChannelConfigReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadChannels(): List<ChannelConfigEntry> {
        val text = context.assets.open("channels.json").bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }
}
