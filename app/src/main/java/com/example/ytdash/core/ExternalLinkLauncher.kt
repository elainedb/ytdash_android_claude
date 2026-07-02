package com.example.ytdash.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single app-root capture point for "open in YouTube" (constitution §4), shared by the list
 * (iteration 2) and the map detail sheet (iteration 4) so there is one `external_open_url` /
 * `external_open_error` banner, not a per-screen duplicate.
 */
@Singleton
class ExternalLinkLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appConfig: AppConfig,
) {
    /** Text = the URL when captured; null when nothing is currently shown. */
    val capturedUrl = mutableStateOf<String?>(null)
    val lastError = mutableStateOf<Boolean>(false)

    fun open(url: String) {
        lastError.value = false
        if (appConfig.captureExternalLinks) {
            capturedUrl.value = url
            return
        }
        capturedUrl.value = null
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            lastError.value = true
        } catch (e: SecurityException) {
            lastError.value = true
        }
    }

    fun dismiss() {
        capturedUrl.value = null
        lastError.value = false
    }
}
