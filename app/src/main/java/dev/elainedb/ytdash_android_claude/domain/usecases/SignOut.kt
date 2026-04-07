package dev.elainedb.ytdash_android_claude.domain.usecases

import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import dev.elainedb.ytdash_android_claude.domain.repository.AuthRepository
import javax.inject.Inject

class SignOut @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<Unit, Unit>() {

    override suspend fun invoke(params: Unit): Result<Unit> {
        return try {
            authRepository.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(dev.elainedb.ytdash_android_claude.core.error.Failure.Auth("Sign-out failed: ${e.message}"))
        }
    }
}
