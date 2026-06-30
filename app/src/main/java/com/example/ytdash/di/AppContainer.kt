package com.example.ytdash.di

import android.content.Context
import androidx.room.Room
import com.example.ytdash.config.Channel
import com.example.ytdash.config.ChannelsLoader
import com.example.ytdash.config.TestConfig
import com.example.ytdash.data.local.AppDatabase
import com.example.ytdash.data.remote.RemoteDataSource
import com.example.ytdash.data.remote.YouTubeApi
import com.example.ytdash.data.repo.VideoRepositoryImpl
import com.example.ytdash.domain.VideoRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Manual dependency-injection container: explicit constructor wiring, presentation depends only on
 * the [VideoRepository] abstraction. Chosen over an annotation-processing DI framework to keep the
 * dependency graph small, fully visible, and build-light (see plan.md).
 */
class AppContainer(private val appContext: Context) {

    val channels: List<Channel> by lazy { ChannelsLoader.load(appContext) }

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "ytdash.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /** Build a repository bound to the runtime base URL + key from the launch extras. */
    fun repository(config: TestConfig): VideoRepository {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val base = config.baseUrl.trimEnd('/') + "/youtube/v3/"
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(YouTubeApi::class.java)
        val remote = RemoteDataSource(api, config.key)
        return VideoRepositoryImpl(remote, database.videoDao(), channels)
    }
}
