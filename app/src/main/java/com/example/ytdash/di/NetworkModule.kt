package com.example.ytdash.di

import com.example.ytdash.BuildConfig
import com.example.ytdash.data.remote.RuntimeConfigInterceptor
import com.example.ytdash.data.remote.YouTubeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(runtimeConfigInterceptor: RuntimeConfigInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(runtimeConfigInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // Fallback default only — RuntimeConfigInterceptor rewrites scheme/host/port per
            // request when a uiTestMode `apiBaseUrl` override is set (constitution §4).
            .baseUrl("${BuildConfig.DEFAULT_API_BASE_URL}/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideYouTubeApi(retrofit: Retrofit): YouTubeApi = retrofit.create(YouTubeApi::class.java)
}
