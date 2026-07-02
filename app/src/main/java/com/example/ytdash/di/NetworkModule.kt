package com.example.ytdash.di

import com.example.ytdash.core.AppConfig
import com.example.ytdash.data.remote.YouTubeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /** Appends the runtime-resolved API key (constitution §4) to every request. */
    @Provides
    @Singleton
    fun provideApiKeyInterceptor(appConfig: AppConfig): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val url = original.url.newBuilder()
            .setQueryParameter("key", appConfig.apiKey)
            .build()
        chain.proceed(original.newBuilder().url(url).build())
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(apiKeyInterceptor: Interceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    // Retrofit's base URL is read from AppConfig at first injection — MainActivity.onCreate
    // populates AppConfig from the UI-test-mode intent extras synchronously before any
    // ViewModel/repository is created, so this always sees the final, run-specific host.
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json, appConfig: AppConfig): Retrofit {
        val base = appConfig.apiBaseUrl.let { if (it.endsWith("/")) it else "$it/" }
        return Retrofit.Builder()
            .baseUrl(base.toHttpUrl())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideYouTubeApi(retrofit: Retrofit): YouTubeApi = retrofit.create(YouTubeApi::class.java)
}
