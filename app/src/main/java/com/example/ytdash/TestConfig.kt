package com.example.ytdash

import android.content.Intent

/**
 * UI-test-mode configuration (constitution §4). Read from the launching Activity's intent extras so
 * the SAME build can be pointed at the mock server or the real YouTube API without a rebuild.
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
            val e = intent?.extras ?: return TestConfig()
            // Extras may arrive as booleans or as "true"/"false" strings (Maestro sends strings).
            fun bool(key: String): Boolean {
                if (e.containsKey(key)) {
                    val v = e.get(key)
                    if (v is Boolean) return v
                    if (v is String) return v.equals("true", ignoreCase = true) || v == "1"
                }
                return false
            }
            fun str(key: String): String? = (e.get(key) as? String)?.takeIf { it.isNotBlank() }
            return TestConfig(
                uiTestMode = bool("uiTestMode"),
                mockAuthEmail = str("mockAuthEmail"),
                apiBaseUrl = str("apiBaseUrl"),
                apiKey = str("apiKey"),
                authorizedEmails = str("authorizedEmails"),
                captureExternalLinks = bool("captureExternalLinks"),
            )
        }
    }
}
