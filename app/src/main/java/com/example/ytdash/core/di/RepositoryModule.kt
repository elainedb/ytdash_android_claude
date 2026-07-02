package com.example.ytdash.core.di

import com.example.ytdash.data.repository.AuthRepositoryImpl
import com.example.ytdash.data.repository.VideoRepositoryImpl
import com.example.ytdash.domain.repository.AuthRepository
import com.example.ytdash.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
