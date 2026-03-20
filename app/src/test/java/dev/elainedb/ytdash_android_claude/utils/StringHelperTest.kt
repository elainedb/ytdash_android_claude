package dev.elainedb.ytdash_android_claude.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringHelperTest {

    @Test
    fun `isPalindrome returns true for simple palindrome`() {
        assertTrue(StringHelper.isPalindrome("racecar"))
    }

    @Test
    fun `isPalindrome ignores case and spaces`() {
        assertTrue(StringHelper.isPalindrome("A man a plan a canal Panama"))
    }

    @Test
    fun `isPalindrome returns false for non-palindrome`() {
        assertFalse(StringHelper.isPalindrome("hello"))
    }

    @Test
    fun `isPalindrome handles empty string`() {
        assertTrue(StringHelper.isPalindrome(""))
    }

    @Test
    fun `wordCount returns correct count`() {
        assertEquals(3, StringHelper.wordCount("hello world test"))
    }

    @Test
    fun `wordCount handles multiple spaces`() {
        assertEquals(2, StringHelper.wordCount("hello   world"))
    }

    @Test
    fun `wordCount returns zero for blank string`() {
        assertEquals(0, StringHelper.wordCount(""))
        assertEquals(0, StringHelper.wordCount("   "))
    }

    @Test
    fun `reverseWords reverses word order`() {
        assertEquals("world hello", StringHelper.reverseWords("hello world"))
    }

    @Test
    fun `capitalizeWords capitalizes first letter of each word`() {
        assertEquals("Hello World", StringHelper.capitalizeWords("hello world"))
    }

    @Test
    fun `removeVowels removes all vowels`() {
        assertEquals("hll wrld", StringHelper.removeVowels("hello world"))
    }

    @Test
    fun `removeVowels handles uppercase vowels`() {
        assertEquals("HLL", StringHelper.removeVowels("HAELLO"))
    }

    @Test
    fun `isValidEmail accepts valid email`() {
        assertTrue(StringHelper.isValidEmail("user@example.com"))
    }

    @Test
    fun `isValidEmail rejects missing at sign`() {
        assertFalse(StringHelper.isValidEmail("userexample.com"))
    }

    @Test
    fun `isValidEmail rejects missing domain`() {
        assertFalse(StringHelper.isValidEmail("user@"))
    }

    @Test
    fun `isValidEmail rejects missing TLD`() {
        assertFalse(StringHelper.isValidEmail("user@example"))
    }
}
