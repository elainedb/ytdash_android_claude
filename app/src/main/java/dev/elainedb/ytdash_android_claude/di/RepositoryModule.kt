package dev.elainedb.ytdash_android_claude.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.elainedb.ytdash_android_claude.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.data.remote.YouTubeApiService
import dev.elainedb.ytdash_android_claude.data.repository.AuthRepositoryImpl
import dev.elainedb.ytdash_android_claude.data.repository.YouTubeRepositoryImpl
import dev.elainedb.ytdash_android_claude.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideVideoRepository(
        apiService: YouTubeApiService,
        videoDao: VideoDao,
        @ApplicationContext context: Context
    ): VideoRepository {
        return YouTubeRepositoryImpl(apiService, videoDao, context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepositoryImpl(context)
    }
}
