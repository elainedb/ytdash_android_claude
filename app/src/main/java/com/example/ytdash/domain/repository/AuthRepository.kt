package com.example.ytdash.domain.repository

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentEmail: StateFlow<String?>
    suspend fun signInWithGoogle(context: Context): Result<String>
    fun signInAs(email: String)
    fun signOut()
}
