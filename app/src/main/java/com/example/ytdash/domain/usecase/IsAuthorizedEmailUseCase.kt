package com.example.ytdash.domain.usecase

import javax.inject.Inject

/** Whitelist check (constitution §2/§4). Email comparison is case-insensitive and trims whitespace. */
class IsAuthorizedEmailUseCase @Inject constructor() {
    operator fun invoke(authorizedEmails: List<String>, email: String): Boolean {
        val normalized = email.trim()
        if (normalized.isEmpty()) return false
        return authorizedEmails.any { it.trim().equals(normalized, ignoreCase = true) }
    }
}
