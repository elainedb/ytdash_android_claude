package com.example.ytdash.data.remote

import com.example.ytdash.BuildConfig
import com.example.ytdash.testmode.TestConfigProvider
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Applies the runtime `apiBaseUrl`/`apiKey` UI-test-mode overrides (constitution §4) to every
 * outgoing request, so Retrofit's compile-time base URL is only ever a fallback default and the
 * API key is genuinely read at RUNTIME (not baked in at compile time only). Retrofit's own base
 * URL stays fixed at construction; this interceptor rewrites scheme/host/port per-request instead
 * of rebuilding Retrofit, so a single build can point at the mock or the real API without a
 * rebuild.
 */
class RuntimeConfigInterceptor @Inject constructor(
    private val testConfigProvider: TestConfigProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val cfg = testConfigProvider.current

        var urlBuilder = original.url.newBuilder()
        val baseOverride = cfg.apiBaseUrl?.takeIf { it.isNotBlank() }?.toHttpUrlOrNull()
        if (baseOverride != null) {
            urlBuilder = original.url.newBuilder()
                .scheme(baseOverride.scheme)
                .host(baseOverride.host)
                .port(baseOverride.port)
        }

        val apiKey = cfg.apiKey?.takeIf { it.isNotBlank() } ?: BuildConfig.YOUTUBE_API_KEY
        val newUrl = urlBuilder.setQueryParameter("key", apiKey).build()

        val newRequest = original.newBuilder().url(newUrl).build()
        return chain.proceed(newRequest)
    }
}
