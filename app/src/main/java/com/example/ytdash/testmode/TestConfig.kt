package com.example.ytdash.testmode

import android.content.Intent

/**
 * The UI-test-mode contract (constitution §4): launch-intent extras that make the app
 * deterministic for automated (Maestro) validation. Outside of `uiTestMode`, the app runs with
 * real Google sign-in, the production API base URL/key, and real external launches.
 */
data class TestConfig(
    val uiTestMode: Boolean = false,
    val mockAuthEmail: String? = null,
    val apiBaseUrl: String? = null,
    val apiKey: String? = null,
    val authorizedEmails: String? = null,
    val captureExternalLinks: Boolean = false,
) {
    companion object {
        fun fromIntent(intent: Intent?): TestConfig {
            val extras = intent?.extras ?: return TestConfig()
            return TestConfig(
                uiTestMode = extras.getBoolean("uiTestMode", false),
                mockAuthEmail = extras.getString("mockAuthEmail"),
                apiBaseUrl = extras.getString("apiBaseUrl"),
                apiKey = extras.getString("apiKey"),
                authorizedEmails = extras.getString("authorizedEmails"),
                captureExternalLinks = extras.getBoolean("captureExternalLinks", false),
            )
        }
    }
}
