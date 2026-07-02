package com.example.ytdash.core.testmode

import android.content.Intent

/**
 * UI-test-mode contract (constitution §4). Read once from the launching Activity's intent
 * extras. Maestro delivers `launchApp.arguments` as Android intent extras regardless of the
 * key/value's declared type in the flow YAML, so both string and boolean extras are read
 * defensively (Maestro sends booleans as real booleans, but we tolerate string "true"/"false"
 * too in case a caller passes them as strings).
 */
data class TestConfig(
    val uiTestMode: Boolean,
    val mockAuthEmail: String?,
    val apiBaseUrl: String?,
    val apiKey: String?,
    val authorizedEmails: List<String>?,
    val captureExternalLinks: Boolean,
) {
    companion object {
        val NONE = TestConfig(
            uiTestMode = false,
            mockAuthEmail = null,
            apiBaseUrl = null,
            apiKey = null,
            authorizedEmails = null,
            captureExternalLinks = false,
        )

        fun fromIntent(intent: Intent?): TestConfig {
            val extras = intent?.extras ?: return NONE
            return TestConfig(
                uiTestMode = extras.getBooleanFlexible("uiTestMode"),
                mockAuthEmail = extras.getString("mockAuthEmail")?.takeIf { it.isNotBlank() },
                apiBaseUrl = extras.getString("apiBaseUrl")?.takeIf { it.isNotBlank() },
                apiKey = extras.getString("apiKey")?.takeIf { it.isNotBlank() },
                authorizedEmails = extras.getString("authorizedEmails")
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.takeIf { it.isNotEmpty() },
                captureExternalLinks = extras.getBooleanFlexible("captureExternalLinks"),
            )
        }

        private fun android.os.Bundle.getBooleanFlexible(key: String): Boolean {
            if (!containsKey(key)) return false
            return try {
                getBoolean(key, false)
            } catch (e: ClassCastException) {
                getString(key)?.toBooleanStrictOrNull() ?: false
            }
        }
    }
}
