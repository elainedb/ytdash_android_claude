package dev.elainedb.ytdash_android_claude

import android.content.Context
import java.util.Properties

object ConfigHelper {
    private var properties: Properties? = null

    private fun loadProperties(context: Context): Properties {
        properties?.let { return it }

        val props = Properties()
        val assetManager = context.assets

        val configFiles = listOf(
            "config.properties",
            "config.properties.ci",
            "config.properties.template"
        )

        for (configFile in configFiles) {
            try {
                assetManager.open(configFile).use { stream ->
                    props.load(stream)
                    properties = props
                    return props
                }
            } catch (_: Exception) {
                // Try next file in the chain
            }
        }

        // Hardcoded fallback values
        props.setProperty("authorized_emails", "")
        props.setProperty("youtubeApiKey", "")
        properties = props
        return props
    }

    fun getAuthorizedEmails(context: Context): List<String> {
        val props = loadProperties(context)
        val emails = props.getProperty("authorized_emails", "")
        return emails.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun getYouTubeApiKey(context: Context): String {
        val props = loadProperties(context)
        return props.getProperty("youtubeApiKey", "")
    }
}
