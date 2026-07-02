package com.example.ytdash.domain.usecase

/**
 * Pure whitelist check (constitution: "access is restricted to a whitelist of authorized
 * emails"). No Android imports — directly unit-testable.
 */
object AuthPolicy {
    fun isAuthorized(email: String, whitelistCsv: String): Boolean {
        if (email.isBlank()) return false
        val whitelist = whitelistCsv.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        return whitelist.contains(email.trim().lowercase())
    }
}
