package dev.elainedb.ytdash_android_claude.config

import android.content.Context
import java.util.Properties

object ConfigHelper {

    private const val CONFIG_FILE = "config.properties"
    private const val CONFIG_CI_FILE = "config.properties.ci"
    private const val CONFIG_TEMPLATE_FILE = "config.properties.template"

    private const val KEY_AUTHORIZED_EMAILS = "authorized_emails"
    private const val KEY_YOUTUBE_API_KEY = "youtubeApiKey"

    private var properties: Properties? = null

    fun getAuthorizedEmails(context: Context): List<String> {
        val props = loadProperties(context)
        val emails = props.getProperty(KEY_AUTHORIZED_EMAILS, "")
        return if (emails.isNotBlank()) {
            emails.split(",").map { it.trim() }
        } else {
            emptyList()
        }
    }

    fun getYouTubeApiKey(context: Context): String {
        val props = loadProperties(context)
        return props.getProperty(KEY_YOUTUBE_API_KEY, "")
    }

    private fun loadProperties(context: Context): Properties {
        properties?.let { return it }

        val props = Properties()
        val filesToTry = listOf(CONFIG_FILE, CONFIG_CI_FILE, CONFIG_TEMPLATE_FILE)

        for (fileName in filesToTry) {
            try {
                context.assets.open(fileName).use { stream ->
                    props.load(stream)
                }
                if (props.containsKey(KEY_AUTHORIZED_EMAILS) || props.containsKey(KEY_YOUTUBE_API_KEY)) {
                    properties = props
                    return props
                }
            } catch (_: Exception) {
                // Try next file
            }
        }

        properties = props
        return props
    }
}
