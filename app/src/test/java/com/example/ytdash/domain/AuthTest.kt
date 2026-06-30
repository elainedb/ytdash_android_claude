package com.example.ytdash.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTest {
    private val whitelist = listOf("elaine.batista1105@gmail.com", "edbpmc@gmail.com")

    @Test
    fun authorizedEmail_isAllowed() {
        assertTrue(Auth.isAuthorized("edbpmc@gmail.com", whitelist))
    }

    @Test
    fun authorizedEmail_isCaseInsensitiveAndTrimmed() {
        assertTrue(Auth.isAuthorized("  EDBPMC@Gmail.com ", whitelist))
    }

    @Test
    fun unauthorizedEmail_isDenied() {
        assertFalse(Auth.isAuthorized("intruder@example.com", whitelist))
    }

    @Test
    fun nullOrBlankEmail_isDenied() {
        assertFalse(Auth.isAuthorized(null, whitelist))
        assertFalse(Auth.isAuthorized("", whitelist))
        assertFalse(Auth.isAuthorized("   ", whitelist))
    }
}
