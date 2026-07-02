package com.example.ytdash.domain

/** Pure whitelist check — no Android dependency, unit-testable in isolation. */
object WhitelistValidator {
    fun isAuthorized(email: String, authorizedEmails: List<String>): Boolean {
        val normalized = email.trim().lowercase()
        return authorizedEmails.any { it.trim().lowercase() == normalized }
    }

    fun parseCsv(csv: String): List<String> =
        csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
