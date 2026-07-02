package com.example.ytdash.di

import com.example.ytdash.data.repo.AuthRepositoryImpl
import com.example.ytdash.data.repo.VideoRepositoryImpl
import com.example.ytdash.domain.repo.AuthRepository
import com.example.ytdash.domain.repo.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Dependency inversion (constitution §1.2): presentation depends on these interfaces only. */
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
