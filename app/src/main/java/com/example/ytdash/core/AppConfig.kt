package com.example.ytdash.core

import com.example.ytdash.BuildConfig
import com.example.ytdash.domain.WhitelistValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime-resolved app configuration: UI-test-mode extras (constitution §4) override the
 * production [BuildConfig] defaults. Populated exactly once, synchronously, in
 * `MainActivity.onCreate` before any screen/ViewModel/repository is created — so every Hilt
 * provider that reads it (network base URL, whitelist) always sees the final values.
 */
@Singleton
class AppConfig @Inject constructor() {
    var uiTestMode: Boolean = false
        private set
    var mockAuthEmail: String? = null
        private set
    var apiBaseUrl: String = BuildConfig.YOUTUBE_API_BASE_URL
        private set
    var apiKey: String = BuildConfig.YOUTUBE_API_KEY
        private set
    var authorizedEmails: List<String> = WhitelistValidator.parseCsv(BuildConfig.AUTHORIZED_EMAILS)
        private set
    var captureExternalLinks: Boolean = false
        private set

    fun applyTestConfig(cfg: TestConfig) {
        uiTestMode = cfg.uiTestMode
        if (!cfg.uiTestMode) return
        mockAuthEmail = cfg.mockAuthEmail
        cfg.apiBaseUrl?.takeIf { it.isNotBlank() }?.let { apiBaseUrl = it }
        cfg.apiKey?.takeIf { it.isNotBlank() }?.let { apiKey = it }
        cfg.authorizedEmails?.takeIf { it.isNotBlank() }?.let {
            authorizedEmails = WhitelistValidator.parseCsv(it)
        }
        captureExternalLinks = cfg.captureExternalLinks
    }
}
