package com.example.ytdash.core

import android.content.Intent

/**
 * UI-test-mode contract (constitution §4): parsed once from the launching Activity's intent
 * extras. Maestro `launchApp.arguments` arrive as Android intent extras regardless of framework.
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
