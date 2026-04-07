package dev.elainedb.ytdash_android_claude.auth.domain.usecase

import android.content.Intent
import dev.elainedb.ytdash_android_claude.auth.domain.model.User
import dev.elainedb.ytdash_android_claude.auth.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_claude.core.error.Failure
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<User, Intent?>() {

    override suspend fun invoke(params: Intent?): Result<User> {
        val result = authRepository.handleSignInResult(params)
        return when (result) {
            is Result.Success -> {
                if (authRepository.isEmailAuthorized(result.data.email)) {
                    result
                } else {
                    authRepository.signOut()
                    Result.Error(Failure.Auth("Access denied. Your email is not authorized."))
                }
            }
            is Result.Error -> result
        }
    }
}
