package com.example.ytdash.data.channels

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * There is NO catch-all "all channels" endpoint (real YouTube has none — spec/youtube-api.md).
 * The set of source channels to aggregate is DATA (config/channels.json, bundled as an asset at
 * build time), never hardcoded — the app must iterate every configured channel and merge/dedupe.
 */
@Singleton
class ChannelsConfigProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val channels: List<ChannelConfig> by lazy {
        context.assets.open("channels.json").use { stream ->
            json.decodeFromString<List<ChannelConfig>>(stream.readBytes().decodeToString())
        }
    }

    fun channels(): List<ChannelConfig> = channels
}
