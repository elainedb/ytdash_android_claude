package dev.elainedb.ytdash_android_claude.auth.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dev.elainedb.ytdash_android_claude.auth.domain.model.User
import dev.elainedb.ytdash_android_claude.auth.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_claude.config.ConfigHelper
import dev.elainedb.ytdash_android_claude.core.error.Failure
import dev.elainedb.ytdash_android_claude.core.error.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleSignInClient: GoogleSignInClient
) : AuthRepository {

    override fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    override suspend fun handleSignInResult(data: Intent?): Result<User> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            val user = User(
                email = account.email ?: "",
                displayName = account.displayName,
                photoUrl = account.photoUrl?.toString()
            )
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(Failure.Auth("Sign-in failed: ${e.message}"))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            googleSignInClient.signOut().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Failure.Auth("Sign-out failed: ${e.message}"))
        }
    }

    override fun isEmailAuthorized(email: String): Boolean {
        val authorizedEmails = ConfigHelper.getAuthorizedEmails(context)
        return authorizedEmails.any { it.equals(email, ignoreCase = true) }
    }
}
