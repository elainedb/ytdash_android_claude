package com.example.ytdash.domain

/** Access control: a signed-in email is allowed only if it is on the whitelist (case-insensitive). */
class AuthService(authorizedEmailsCsv: String?) {

    private val whitelist: Set<String> =
        authorizedEmailsCsv.orEmpty()
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

    fun isAuthorized(email: String?): Boolean {
        val e = email?.trim()?.lowercase().orEmpty()
        return e.isNotEmpty() && e in whitelist
    }
}
