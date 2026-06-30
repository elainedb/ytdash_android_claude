package com.example.ytdash.config

import android.content.Intent
import android.os.Bundle
import com.example.ytdash.BuildConfig

/**
 * UI-test-mode contract (constitution §4). Values arrive as launch-intent extras so the harness can
 * point the same build at a mock or the real API, and drive deterministic sign-in / external-open.
 * Outside UI-test-mode the app behaves normally (real Google sign-in, real external launch).
 */
data class TestConfig(
    val uiTestMode: Boolean,
    val mockAuthEmail: String?,
    val apiBaseUrl: String?,
    val apiKey: String?,
    val authorizedEmails: List<String>,
    val captureExternalLinks: Boolean,
) {
    /** Host root only; the data layer appends /youtube/v3/<endpoint> itself. */
    val baseUrl: String get() = apiBaseUrl?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_API_BASE

    /** Read at RUNTIME so one build serves both the mock and the real API. */
    val key: String get() = apiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.YOUTUBE_API_KEY

    companion object {
        fun fromIntent(intent: Intent?): TestConfig {
            val e = intent?.extras
            val authorized = e?.string("authorizedEmails")
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
                ?: BuildConfig.DEFAULT_AUTHORIZED_EMAILS.split(",").map { it.trim() }
            return TestConfig(
                uiTestMode = e.bool("uiTestMode", false),
                mockAuthEmail = e?.string("mockAuthEmail"),
                apiBaseUrl = e?.string("apiBaseUrl"),
                apiKey = e?.string("apiKey"),
                authorizedEmails = authorized,
                captureExternalLinks = e.bool("captureExternalLinks", false),
            )
        }

        // Maestro may deliver typed extras as Boolean/String depending on the value; handle both.
        private fun Bundle?.bool(key: String, def: Boolean): Boolean = when (val v = this?.get(key)) {
            is Boolean -> v
            is String -> v.equals("true", true) || v == "1"
            else -> def
        }

        private fun Bundle.string(key: String): String? = when (val v = get(key)) {
            is String -> v.takeIf { it.isNotBlank() }
            null -> null
            else -> v.toString()
        }
    }
}
