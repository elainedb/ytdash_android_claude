package dev.elainedb.ytdash_android_claude.core.error

sealed class Failure(val message: String) {
    class Server(message: String) : Failure(message)
    class Cache(message: String) : Failure(message)
    class Network(message: String) : Failure(message)
    class Auth(message: String) : Failure(message)
    class Validation(message: String) : Failure(message)
    class Unexpected(message: String) : Failure(message)
}
