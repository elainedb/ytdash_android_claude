package dev.elainedb.ytdash_android_claude.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.elainedb.ytdash_android_claude.data.repository.AuthRepositoryImpl
import dev.elainedb.ytdash_android_claude.data.repository.YouTubeRepositoryImpl
import dev.elainedb.ytdash_android_claude.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: YouTubeRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
