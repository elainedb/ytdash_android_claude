package dev.elainedb.ytdash_android_claude.utils

object StringHelper {

    fun isPalindrome(text: String): Boolean {
        val cleaned = text.lowercase().filter { it.isLetterOrDigit() }
        return cleaned == cleaned.reversed()
    }

    fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split("\\s+".toRegex()).size
    }

    fun reverseWords(text: String): String {
        return text.trim().split("\\s+".toRegex()).reversed().joinToString(" ")
    }

    fun capitalizeWords(text: String): String {
        return text.split("\\s+".toRegex()).joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }

    fun removeVowels(text: String): String {
        return text.filter { it.lowercaseChar() !in "aeiou" }
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
}
