package dev.elainedb.ytdash_android_claude.core.usecases

import dev.elainedb.ytdash_android_claude.core.error.Result

abstract class UseCase<out T, in P> {
    abstract suspend operator fun invoke(params: P): Result<T>
}
