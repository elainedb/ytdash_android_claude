package dev.elainedb.ytdash_android_claude.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.elainedb.ytdash_android_claude.video.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.video.data.local.VideoDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVideoDatabase(@ApplicationContext context: Context): VideoDatabase {
        return VideoDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: VideoDatabase): VideoDao {
        return database.videoDao()
    }
}
