package com.example.ytdash.data.auth

import android.content.Context
import android.content.Intent
import com.example.ytdash.core.AppConfig
import com.example.ytdash.domain.WhitelistValidator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthOutcome {
    data class Authorized(val email: String) : AuthOutcome
    data class Unauthorized(val email: String) : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}

/**
 * Mock path (uiTestMode + mockAuthEmail) and real Google Sign-In path both funnel through the
 * same [WhitelistValidator] logic (constitution §4: "UI test mode must not weaken production
 * behavior — it only swaps non-deterministic edges for testable ones").
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appConfig: AppConfig,
) {
    private val prefs by lazy { context.getSharedPreferences("ytdash_auth", Context.MODE_PRIVATE) }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val isMockMode: Boolean get() = appConfig.uiTestMode

    fun currentEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun signInWithMock(): AuthOutcome = resolve(appConfig.mockAuthEmail.orEmpty())

    fun googleSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleGoogleSignInResult(data: Intent?): AuthOutcome = try {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data)
            .getResult(ApiException::class.java)
        resolve(account?.email.orEmpty())
    } catch (e: ApiException) {
        AuthOutcome.Failure(e.message ?: "Google sign-in failed")
    }

    fun signOut() {
        prefs.edit().remove(KEY_EMAIL).apply()
        if (!appConfig.uiTestMode) {
            googleSignInClient.signOut()
        }
    }

    private fun resolve(email: String): AuthOutcome {
        if (email.isBlank()) return AuthOutcome.Failure("No email returned")
        return if (WhitelistValidator.isAuthorized(email, appConfig.authorizedEmails)) {
            prefs.edit().putString(KEY_EMAIL, email).apply()
            AuthOutcome.Authorized(email)
        } else {
            AuthOutcome.Unauthorized(email)
        }
    }

    private companion object {
        const val KEY_EMAIL = "email"
    }
}
