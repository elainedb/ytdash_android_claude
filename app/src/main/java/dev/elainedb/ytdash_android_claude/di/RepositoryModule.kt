package dev.elainedb.ytdash_android_claude.di

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.elainedb.ytdash_android_claude.auth.data.repository.AuthRepositoryImpl
import dev.elainedb.ytdash_android_claude.auth.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_claude.video.data.repository.YouTubeRepositoryImpl
import dev.elainedb.ytdash_android_claude.video.domain.repository.VideoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: YouTubeRepositoryImpl): VideoRepository

    companion object {
        @Provides
        @Singleton
        fun provideGoogleSignInClient(@ApplicationContext context: Context): GoogleSignInClient {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            return GoogleSignIn.getClient(context, gso)
        }
    }
}
