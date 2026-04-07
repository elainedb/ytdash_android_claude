package dev.elainedb.ytdash_android_claude.domain.usecases

import android.content.Intent
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import dev.elainedb.ytdash_android_claude.domain.model.User
import dev.elainedb.ytdash_android_claude.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<User, Intent?>() {

    override suspend fun invoke(params: Intent?): Result<User> {
        return authRepository.handleSignInResult(params)
    }

    fun getSignInIntent(): Intent = authRepository.getSignInIntent()
}
