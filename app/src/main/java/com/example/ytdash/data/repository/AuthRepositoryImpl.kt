package com.example.ytdash.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.ytdash.BuildConfig
import com.example.ytdash.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real sign-in uses Credential Manager's Google ID flow (constitution: production behavior must
 * work unmodified; UI-test-mode only swaps the account picker for `mockAuthEmail`, it never
 * changes this class's contract). Requires a Google web (server) client id — provided via
 * config/secrets.env (`GOOGLE_WEB_CLIENT_ID`, gitignored, never committed). If it isn't
 * configured, sign-in fails with a clear message instead of crashing.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _currentEmail = MutableStateFlow<String?>(null)
    override val currentEmail: StateFlow<String?> = _currentEmail.asStateFlow()

    override suspend fun signInWithGoogle(context: Context): Result<String> = runCatching {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        check(webClientId.isNotBlank()) {
            "Google Sign-In is not configured (missing GOOGLE_WEB_CLIENT_ID)."
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        val response = credentialManager.getCredential(context, request)
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        googleIdTokenCredential.id
    }

    override fun signInAs(email: String) {
        _currentEmail.value = email
    }

    override fun signOut() {
        _currentEmail.value = null
    }
}
