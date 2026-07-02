package com.example.ytdash.di

import android.content.Context
import com.example.ytdash.BuildConfig
import com.example.ytdash.TestConfig
import com.example.ytdash.data.ChannelConfig
import com.example.ytdash.data.DefaultVideoRepository
import com.example.ytdash.data.VideoRepository
import com.example.ytdash.data.cache.FileVideoCache
import com.example.ytdash.data.model.SourceChannel
import com.example.ytdash.data.remote.YouTubeApi
import com.example.ytdash.domain.AuthService
import com.example.ytdash.ui.ExternalLauncher
import com.example.ytdash.ui.RealExternalLauncher

/**
 * Minimal, hand-rolled dependency container (dependency inversion without an annotation processor).
 * Presentation depends on the interfaces (VideoRepository, ExternalLauncher, AuthService), which are
 * constructed here from the runtime [TestConfig].
 */
class AppContainer(appContext: Context, config: TestConfig) {

    // Production config comes from build-time secrets (BuildConfig), never source; UI-test-mode
    // extras override both at runtime (constitution §4).
    private val productionBaseUrl = "https://www.googleapis.com"

    val channels: List<SourceChannel> = ChannelConfig.load(appContext)

    val authService: AuthService =
        AuthService(config.authorizedEmails ?: BuildConfig.AUTHORIZED_EMAILS)

    val externalLauncher: ExternalLauncher = RealExternalLauncher(appContext.applicationContext)

    private val api = YouTubeApi(
        baseUrl = config.apiBaseUrl ?: productionBaseUrl,
        apiKey = config.apiKey ?: BuildConfig.YOUTUBE_API_KEY.ifBlank { null },
    )

    val repository: VideoRepository =
        DefaultVideoRepository(
            channels = channels,
            api = api,
            cache = FileVideoCache(appContext.applicationContext),
        )
}
