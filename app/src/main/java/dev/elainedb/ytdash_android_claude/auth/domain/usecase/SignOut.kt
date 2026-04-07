package dev.elainedb.ytdash_android_claude.auth.domain.usecase

import dev.elainedb.ytdash_android_claude.auth.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import javax.inject.Inject

class SignOut @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<Unit, Unit>() {

    override suspend fun invoke(params: Unit): Result<Unit> {
        return authRepository.signOut()
    }
}
