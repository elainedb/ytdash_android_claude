package com.example.ytdash.core.link

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.ytdash.core.config.RuntimeConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-root shared state (constitution §4 `captureExternalLinks`) — lifted above both the list
 * and the map bottom sheet so the SAME `external_open_url`/`external_open_error` banner serves
 * both call sites without duplicating the capture/real-launch logic per screen. Scoped to the
 * hosting Activity (see MainActivity) so both screens' composables resolve to this one instance.
 */
@HiltViewModel
class ExternalLinkViewModel @Inject constructor(
    private val runtimeConfig: RuntimeConfig,
) : ViewModel() {

    private val _event = MutableStateFlow<ExternalLinkEvent?>(null)
    val event: StateFlow<ExternalLinkEvent?> = _event.asStateFlow()

    fun openVideo(context: Context, url: String) {
        val captureExternalLinks = runtimeConfig.state.value.captureExternalLinks
        if (captureExternalLinks) {
            _event.value = ExternalLinkEvent.Captured(url)
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            _event.value = ExternalLinkEvent.Error
        } catch (e: Exception) {
            _event.value = ExternalLinkEvent.Error
        }
    }
}
