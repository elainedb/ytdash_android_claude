package com.example.ytdash

import com.example.ytdash.domain.AuthService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthServiceTest {

    private val auth = AuthService("elaine.batista1105@gmail.com, edbpmc@gmail.com")

    @Test
    fun authorizedEmailIsAllowed() {
        assertTrue(auth.isAuthorized("edbpmc@gmail.com"))
        assertTrue(auth.isAuthorized("elaine.batista1105@gmail.com"))
    }

    @Test
    fun authorizationIsCaseInsensitiveAndTrimmed() {
        assertTrue(auth.isAuthorized("  EDBPMC@Gmail.com "))
    }

    @Test
    fun unauthorizedEmailIsDenied() {
        assertFalse(auth.isAuthorized("intruder@example.com"))
        assertFalse(auth.isAuthorized(null))
        assertFalse(auth.isAuthorized(""))
    }

    @Test
    fun emptyWhitelistDeniesEveryone() {
        val empty = AuthService(null)
        assertFalse(empty.isAuthorized("edbpmc@gmail.com"))
    }
}
