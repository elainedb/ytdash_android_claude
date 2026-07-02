package com.example.ytdash.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens a URL in an external app. Returns false if the launch fails (no crash / no silent no-op). */
interface ExternalLauncher {
    fun open(url: String): Boolean
}

class RealExternalLauncher(private val appContext: Context) : ExternalLauncher {
    override fun open(url: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
