package dev.elainedb.ytdash_android_claude.domain.repository

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.domain.model.User

interface AuthRepository {
    fun getSignInClient(): GoogleSignInClient
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Result<User>
    fun isEmailAuthorized(email: String): Boolean
    suspend fun signOut()
}
