package com.example.ytdash.domain.repo

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Session persistence + sign-in mechanics. The email-whitelist decision itself is a separate,
 * pure, unit-testable use-case ([com.example.ytdash.domain.usecase.AuthPolicy]); the ViewModel
 * runs that check BEFORE calling [persistSession], so an unauthorized email is never persisted as
 * a signed-in session — see LoginViewModel.
 */
interface AuthRepository {
    val sessionEmail: Flow<String?>

    /** Real Google sign-in via Credential Manager; returns the signed-in account's email. Does
     *  NOT persist a session — the caller decides that after the whitelist check. */
    suspend fun signInWithGoogle(context: Context): Result<String>

    suspend fun persistSession(email: String)

    suspend fun signOut()
}
