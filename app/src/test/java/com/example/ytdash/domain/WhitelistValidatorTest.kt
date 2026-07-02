package com.example.ytdash.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitelistValidatorTest {

    private val whitelist = listOf("allow@example.com", "Second@Example.com")

    @Test
    fun `authorized email on the whitelist is accepted`() {
        assertTrue(WhitelistValidator.isAuthorized("allow@example.com", whitelist))
    }

    @Test
    fun `whitelist check is case-insensitive`() {
        assertTrue(WhitelistValidator.isAuthorized("ALLOW@EXAMPLE.COM", whitelist))
        assertTrue(WhitelistValidator.isAuthorized("second@example.com", whitelist))
    }

    @Test
    fun `email not on the whitelist is rejected`() {
        assertFalse(WhitelistValidator.isAuthorized("deny@example.com", whitelist))
    }

    @Test
    fun `blank email is rejected`() {
        assertFalse(WhitelistValidator.isAuthorized("", whitelist))
    }

    @Test
    fun `parseCsv splits trims and drops blanks`() {
        val parsed = WhitelistValidator.parseCsv(" a@x.com ,b@x.com,, c@x.com")
        assertEquals(listOf("a@x.com", "b@x.com", "c@x.com"), parsed)
    }
}
