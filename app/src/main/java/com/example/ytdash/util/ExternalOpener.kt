package com.example.ytdash.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Launches an external URL (the real YouTube/browser experience). */
object ExternalOpener {
    /**
     * Returns true if the external app was launched, false if no handler exists or the launch
     * threw — the caller surfaces `external_open_error` in that case rather than crashing or
     * silently no-oping (constitution §6, AC-LINK-01).
     */
    fun launch(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) == null) return false
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
