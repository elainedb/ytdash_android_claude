package com.example.ytdash.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthPolicyTest {

    @Test
    fun `authorized email on whitelist is allowed`() {
        assertTrue(AuthPolicy.isAuthorized("a@example.com", "a@example.com,b@example.com"))
    }

    @Test
    fun `email not on whitelist is denied`() {
        assertFalse(AuthPolicy.isAuthorized("c@example.com", "a@example.com,b@example.com"))
    }

    @Test
    fun `whitelist check is case-insensitive`() {
        assertTrue(AuthPolicy.isAuthorized("A@Example.com", "a@example.com"))
    }

    @Test
    fun `whitelist entries tolerate surrounding whitespace`() {
        assertTrue(AuthPolicy.isAuthorized("b@example.com", "a@example.com, b@example.com ,c@example.com"))
    }

    @Test
    fun `empty whitelist denies everyone`() {
        assertFalse(AuthPolicy.isAuthorized("a@example.com", ""))
    }

    @Test
    fun `blank email is never authorized`() {
        assertFalse(AuthPolicy.isAuthorized("", "a@example.com"))
    }
}
