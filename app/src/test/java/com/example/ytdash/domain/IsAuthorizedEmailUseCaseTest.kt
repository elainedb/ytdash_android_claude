package com.example.ytdash.domain

import com.example.ytdash.domain.usecase.IsAuthorizedEmailUseCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IsAuthorizedEmailUseCaseTest {

    private val useCase = IsAuthorizedEmailUseCase()
    private val whitelist = listOf("elaine.batista1105@gmail.com", "edbpmc@gmail.com")

    @Test
    fun `authorized email on whitelist returns true`() {
        assertThat(useCase(whitelist, "edbpmc@gmail.com")).isTrue()
    }

    @Test
    fun `comparison is case-insensitive`() {
        assertThat(useCase(whitelist, "EdbPMC@Gmail.com")).isTrue()
    }

    @Test
    fun `comparison trims whitespace`() {
        assertThat(useCase(whitelist, "  edbpmc@gmail.com  ")).isTrue()
    }

    @Test
    fun `email not on whitelist returns false`() {
        assertThat(useCase(whitelist, "someone-else@gmail.com")).isFalse()
    }

    @Test
    fun `blank email returns false`() {
        assertThat(useCase(whitelist, "")).isFalse()
        assertThat(useCase(whitelist, "   ")).isFalse()
    }

    @Test
    fun `empty whitelist authorizes nobody`() {
        assertThat(useCase(emptyList(), "edbpmc@gmail.com")).isFalse()
    }
}
