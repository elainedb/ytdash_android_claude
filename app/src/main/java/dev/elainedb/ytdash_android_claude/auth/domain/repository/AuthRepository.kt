package dev.elainedb.ytdash_android_claude.auth.domain.repository

import android.content.Intent
import dev.elainedb.ytdash_android_claude.auth.domain.model.User
import dev.elainedb.ytdash_android_claude.core.error.Result

interface AuthRepository {
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Result<User>
    suspend fun signOut(): Result<Unit>
    fun isEmailAuthorized(email: String): Boolean
}
