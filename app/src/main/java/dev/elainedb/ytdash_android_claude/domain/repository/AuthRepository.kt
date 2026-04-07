package dev.elainedb.ytdash_android_claude.domain.repository

import android.content.Intent
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.domain.model.User

interface AuthRepository {
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Result<User>
    suspend fun signOut(): Result<Unit>
}
