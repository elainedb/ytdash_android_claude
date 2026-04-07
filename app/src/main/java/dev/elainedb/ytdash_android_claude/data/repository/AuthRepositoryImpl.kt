package dev.elainedb.ytdash_android_claude.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.elainedb.ytdash_android_claude.config.ConfigHelper
import dev.elainedb.ytdash_android_claude.core.error.Failure
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.domain.model.User
import dev.elainedb.ytdash_android_claude.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    override fun getSignInClient(): GoogleSignInClient = googleSignInClient

    override fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    override suspend fun handleSignInResult(data: Intent?): Result<User> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.result
            val email = account?.email ?: return Result.Error(Failure.Auth("No email found"))

            if (!isEmailAuthorized(email)) {
                googleSignInClient.signOut().await()
                return Result.Error(Failure.Auth("Access denied. Your email is not authorized."))
            }

            Result.Success(
                User(
                    email = email,
                    displayName = account.displayName,
                    photoUrl = account.photoUrl?.toString()
                )
            )
        } catch (e: Exception) {
            Result.Error(Failure.Auth("Sign-in failed: ${e.message}"))
        }
    }

    override fun isEmailAuthorized(email: String): Boolean {
        val authorizedEmails = ConfigHelper.getAuthorizedEmails(context)
        return authorizedEmails.contains(email)
    }

    override suspend fun signOut() {
        googleSignInClient.signOut().await()
    }
}
