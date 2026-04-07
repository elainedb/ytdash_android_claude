package dev.elainedb.ytdash_android_claude.config

import android.content.Context
import java.util.Properties

object ConfigHelper {

    private var properties: Properties? = null

    private fun loadProperties(context: Context): Properties {
        properties?.let { return it }

        val props = Properties()
        val assetManager = context.assets

        val fileNames = listOf(
            "config.properties",
            "config.properties.ci",
            "config.properties.template"
        )

        for (fileName in fileNames) {
            try {
                assetManager.open(fileName).use { stream ->
                    props.load(stream)
                }
                properties = props
                return props
            } catch (_: Exception) {
                // Try next file
            }
        }

        properties = props
        return props
    }

    fun getAuthorizedEmails(context: Context): List<String> {
        val props = loadProperties(context)
        val emails = props.getProperty("authorized_emails", "")
        if (emails.isBlank()) return emptyList()
        return emails.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getYouTubeApiKey(context: Context): String {
        val props = loadProperties(context)
        return props.getProperty("youtubeApiKey", "")
    }
}
