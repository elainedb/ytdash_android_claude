package com.example.ytdash.config

import android.content.Intent
import com.example.ytdash.BuildConfig

/**
 * UI-test-mode contract (constitution §4). Read once at launch from the Activity intent extras.
 * The same compiled build talks to the mock or the real API purely by swapping [apiBaseUrl] /
 * [apiKey] — nothing is baked in at compile time only.
 */
data class TestConfig(
    val uiTestMode: Boolean,
    val mockAuthEmail: String?,
    val apiBaseUrl: String,
    val apiKey: String,
    val authorizedEmails: List<String>,
    val captureExternalLinks: Boolean,
) {
    companion object {
        // Production fallback whitelist (overridable per-run via the authorizedEmails extra).
        private val DEFAULT_AUTHORIZED = listOf(
            "elaine.batista1105@gmail.com",
            "edbpmc@gmail.com",
        )

        fun fromIntent(intent: Intent?): TestConfig {
            val e = intent?.extras
            val uiTestMode = e?.getBoolean("uiTestMode", false) ?: false

            val baseUrl = e?.getString("apiBaseUrl")?.takeIf { it.isNotBlank() }
                ?: BuildConfig.DEFAULT_API_BASE_URL
            // apiKey: prefer the runtime extra (lets the harness point at the real API without a
            // rebuild); fall back to the build-time key for production.
            val apiKey = e?.getString("apiKey")?.takeIf { it.isNotBlank() }
                ?: BuildConfig.YOUTUBE_API_KEY

            val authorized = e?.getString("authorizedEmails")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_AUTHORIZED

            return TestConfig(
                uiTestMode = uiTestMode,
                mockAuthEmail = e?.getString("mockAuthEmail")?.takeIf { it.isNotBlank() },
                apiBaseUrl = baseUrl.trimEnd('/'),
                apiKey = apiKey,
                authorizedEmails = authorized,
                captureExternalLinks = e?.getBoolean("captureExternalLinks", false) ?: false,
            )
        }
    }
}
