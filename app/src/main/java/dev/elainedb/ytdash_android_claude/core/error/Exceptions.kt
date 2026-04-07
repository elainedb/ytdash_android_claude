package dev.elainedb.ytdash_android_claude.core.error

sealed class AppException(override val message: String) : Exception(message) {
    class ServerException(message: String) : AppException(message)
    class CacheException(message: String) : AppException(message)
    class NetworkException(message: String) : AppException(message)
    class AuthException(message: String) : AppException(message)
}
