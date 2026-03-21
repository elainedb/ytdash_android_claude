package dev.elainedb.ytdash_android_claude.config

import android.content.Context
import java.util.Properties

object ConfigHelper {
    private var properties: Properties? = null

    private val CONFIG_FILES = listOf(
        "config.properties",
        "config.properties.ci",
        "config.properties.template"
    )

    private fun loadProperties(context: Context): Properties {
        properties?.let { return it }

        val props = Properties()
        for (configFile in CONFIG_FILES) {
            try {
                context.assets.open(configFile).use { inputStream ->
                    props.load(inputStream)
                    properties = props
                    return props
                }
            } catch (_: Exception) {
                continue
            }
        }
        properties = props
        return props
    }

    fun getAuthorizedEmails(context: Context): List<String> {
        val props = loadProperties(context)
        val emails = props.getProperty("authorized_emails", "")
        if (emails.isBlank()) return emptyList()
        return emails.split(",").map { it.trim() }
    }

    fun getYouTubeApiKey(context: Context): String {
        val props = loadProperties(context)
        return props.getProperty("youtubeApiKey", "")
    }
}
