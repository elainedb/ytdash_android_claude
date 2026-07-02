package com.example.ytdash.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ytdash.core.link.ExternalLinkEvent

/**
 * Rendered in the MAIN composition tree at the app root (constitution §5a) — NOT a Dialog/
 * Snackbar popup — so `external_open_url`/`external_open_error` stay reachable to the
 * automation layer regardless of which screen (list or map sheet) triggered the open.
 */
@Composable
fun ExternalLinkBanner(event: ExternalLinkEvent?, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = event != null, modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.inverseSurface)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (event) {
                is ExternalLinkEvent.Captured -> Text(
                    text = event.url,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.testTag("external_open_url"),
                )
                ExternalLinkEvent.Error -> Text(
                    text = "Couldn't open the external link.",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.testTag("external_open_error"),
                )
                null -> Unit
            }
        }
    }
}
