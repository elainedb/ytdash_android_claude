package dev.elainedb.ytdash_android_claude.config

import android.content.Context
import java.util.Properties

object ConfigHelper {
    private const val CONFIG_FILE = "config.properties"
    private const val CONFIG_CI_FILE = "config.properties.ci"
    private const val CONFIG_TEMPLATE_FILE = "config.properties.template"

    private const val KEY_AUTHORIZED_EMAILS = "authorized_emails"
    private const val KEY_YOUTUBE_API_KEY = "youtubeApiKey"

    private const val DEFAULT_YOUTUBE_API_KEY = "YOUR_YOUTUBE_API_KEY"

    private var properties: Properties? = null

    fun init(context: Context) {
        properties = Properties()
        val assetManager = context.assets

        val filesToTry = listOf(CONFIG_FILE, CONFIG_CI_FILE, CONFIG_TEMPLATE_FILE)
        for (file in filesToTry) {
            try {
                assetManager.open(file).use { inputStream ->
                    properties?.load(inputStream)
                    return
                }
            } catch (_: Exception) {
                // Try next file
            }
        }
    }

    fun getAuthorizedEmails(): List<String> {
        val emails = properties?.getProperty(KEY_AUTHORIZED_EMAILS) ?: return emptyList()
        return emails.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getYouTubeApiKey(): String {
        return properties?.getProperty(KEY_YOUTUBE_API_KEY) ?: DEFAULT_YOUTUBE_API_KEY
    }

    fun isEmailAuthorized(email: String): Boolean {
        return getAuthorizedEmails().any { it.equals(email, ignoreCase = true) }
    }
}
