package com.example.ytdash.core.config

import com.example.ytdash.BuildConfig
import com.example.ytdash.core.testmode.TestConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single mutable source of truth for runtime-configurable endpoint/auth settings (constitution
 * §2 "configurable endpoints", §4 UI-test-mode contract). Populated once at process start from
 * the launching Activity's intent extras (see MainActivity), then read by the network layer
 * (via an OkHttp interceptor) and the auth layer on every call — never baked in at compile time
 * only, so the SAME build can point at the mock or the real API.
 */
data class RuntimeConfigState(
    val uiTestMode: Boolean = false,
    val mockAuthEmail: String? = null,
    val apiBaseUrl: String = BuildConfig.DEFAULT_API_BASE_URL,
    val apiKey: String = BuildConfig.YOUTUBE_API_KEY,
    val authorizedEmails: List<String> = defaultAuthorizedEmails(),
    val captureExternalLinks: Boolean = false,
) {
    companion object {
        fun defaultAuthorizedEmails(): List<String> =
            BuildConfig.DEFAULT_AUTHORIZED_EMAILS.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Singleton
class RuntimeConfig @Inject constructor() {
    private val _state = MutableStateFlow(RuntimeConfigState())
    val state: StateFlow<RuntimeConfigState> = _state.asStateFlow()

    fun applyTestConfig(testConfig: TestConfig) {
        _state.update { current ->
            current.copy(
                uiTestMode = testConfig.uiTestMode,
                mockAuthEmail = testConfig.mockAuthEmail,
                apiBaseUrl = testConfig.apiBaseUrl ?: current.apiBaseUrl,
                apiKey = testConfig.apiKey ?: current.apiKey,
                authorizedEmails = testConfig.authorizedEmails ?: current.authorizedEmails,
                captureExternalLinks = testConfig.captureExternalLinks,
            )
        }
    }
}
