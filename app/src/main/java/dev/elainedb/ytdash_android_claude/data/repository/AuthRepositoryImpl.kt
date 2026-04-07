package dev.elainedb.ytdash_android_claude.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.elainedb.ytdash_android_claude.core.error.Failure
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.domain.model.User
import dev.elainedb.ytdash_android_claude.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_claude.utils.ConfigHelper
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val context: Context
) : AuthRepository {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    override fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    override suspend fun handleSignInResult(data: Intent?): Result<User> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            val email = account?.email ?: return Result.Error(
                Failure.Auth("No email found in sign-in result")
            )

            val authorizedEmails = ConfigHelper.getAuthorizedEmails(context)
            if (email !in authorizedEmails) {
                googleSignInClient.signOut().await()
                return Result.Error(
                    Failure.Auth("Access denied. Your email is not authorized.")
                )
            }

            Log.d("AuthRepository", "Access granted to $email")
            Result.Success(User(email = email, displayName = account.displayName))
        } catch (e: Exception) {
            Result.Error(Failure.Auth(e.message ?: "Sign-in failed"))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            googleSignInClient.signOut().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(Failure.Auth(e.message ?: "Sign-out failed"))
        }
    }
}
