package com.example.ytdash.domain

/** Access control: a signed-in email is allowed only if it is on the (case-insensitive) whitelist. */
object Auth {
    fun isAuthorized(email: String?, whitelist: List<String>): Boolean {
        if (email.isNullOrBlank()) return false
        val normalized = email.trim().lowercase()
        return whitelist.any { it.trim().lowercase() == normalized }
    }
}
