package com.example.ytdash.core.di

import com.example.ytdash.core.config.RuntimeConfig
import com.example.ytdash.data.remote.YouTubeApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /**
     * The base URL and API key are RESOLVED PER REQUEST from [RuntimeConfig], not baked in at
     * client-construction time — this is what lets one build point at the mock or the real API
     * without a rebuild (constitution §4: `apiBaseUrl`/`apiKey` are runtime launch extras).
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(runtimeConfig: RuntimeConfig): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val config = runtimeConfig.state.value
                val configuredBase = config.apiBaseUrl.toHttpUrlOrNull()
                val original = chain.request()
                val newUrlBuilder = original.url.newBuilder()
                if (configuredBase != null) {
                    newUrlBuilder
                        .scheme(configuredBase.scheme)
                        .host(configuredBase.host)
                        .port(configuredBase.port)
                }
                newUrlBuilder.setQueryParameter("key", config.apiKey)
                chain.proceed(original.newBuilder().url(newUrlBuilder.build()).build())
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://localhost/") // placeholder; rewritten per-request by the interceptor above
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideYouTubeApiService(retrofit: Retrofit): YouTubeApiService =
        retrofit.create(YouTubeApiService::class.java)
}
