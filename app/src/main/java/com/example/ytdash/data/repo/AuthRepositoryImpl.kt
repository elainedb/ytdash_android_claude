package com.example.ytdash.data.repo

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.ytdash.BuildConfig
import com.example.ytdash.domain.repo.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session ONLY, deliberately not persisted across process restarts: every flow in
 * `flows/` re-taps `login_google_button` after a fresh `launchApp` — including the offline-relaunch
 * step of AC-CACHE-01, which relaunches with `clearState:false` (so a disk-persisted session would
 * skip straight past `screen_login`, which the flows don't expect; only the *video* cache, via
 * Room, is meant to survive a relaunch). A `Singleton`-scoped `StateFlow` survives configuration
 * changes/ViewModel recreation within one process, which is all the spec asks for.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    private val _sessionEmail = MutableStateFlow<String?>(null)
    override val sessionEmail: Flow<String?> = _sessionEmail.asStateFlow()

    override suspend fun persistSession(email: String) {
        _sessionEmail.value = email
    }

    override suspend fun signInWithGoogle(context: Context): Result<String> {
        if (BuildConfig.GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google Sign-In is not configured (no GOOGLE_SERVER_CLIENT_ID / " +
                        "google-services.json in this workspace)."
                )
            )
        }
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val response = CredentialManager.create(context).getCredential(context, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.success(googleIdTokenCredential.id)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        _sessionEmail.value = null
    }
}
