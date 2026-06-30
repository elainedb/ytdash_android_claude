package com.example.ytdash.di

import android.content.Context
import com.example.ytdash.config.TestConfig
import com.example.ytdash.data.VideoRepository
import com.example.ytdash.data.VideoRepositoryImpl
import com.example.ytdash.data.local.ChannelConfig
import com.example.ytdash.data.local.VideoCache
import com.example.ytdash.data.remote.YouTubeApi

/**
 * Manual dependency container (the chosen DI mechanism — see plan.md). Built once per Activity
 * launch from the current [TestConfig], so the apiBaseUrl/apiKey from the launch extras flow into
 * the network layer without a rebuild. Presentation depends on the [VideoRepository] abstraction,
 * not on these concretions (constitution §1.2).
 */
class AppContainer(context: Context, val config: TestConfig) {
    private val appContext = context.applicationContext

    val channels = ChannelConfig.load(appContext)
    private val cache = VideoCache(appContext)
    private val api = YouTubeApi(config.apiBaseUrl, config.apiKey)

    val repository: VideoRepository = VideoRepositoryImpl(api, cache, channels)
}
