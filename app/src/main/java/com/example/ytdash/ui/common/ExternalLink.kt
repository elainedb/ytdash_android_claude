package com.example.ytdash.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Constitution §4 `captureExternalLinks`: `true` renders [ExternalLinkState.Captured] (text =
 * the target URL) instead of launching, for a deterministic assertion; `false` performs the real
 * external launch and — critically — never crashes or silently no-ops on failure, surfacing
 * [ExternalLinkState.Error] instead (constitution §1.6, AC-LINK-01).
 */
sealed interface ExternalLinkState {
    data object Idle : ExternalLinkState
    data class Captured(val url: String) : ExternalLinkState
    data object Error : ExternalLinkState
}

object ExternalLinkLauncher {
    fun launch(context: Context, url: String, captureExternalLinks: Boolean): ExternalLinkState {
        if (captureExternalLinks) {
            return ExternalLinkState.Captured(url)
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ExternalLinkState.Idle
        } catch (e: ActivityNotFoundException) {
            ExternalLinkState.Error
        } catch (e: Exception) {
            ExternalLinkState.Error
        }
    }
}
