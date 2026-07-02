package com.example.ytdash.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Lifted to the app root (cross-framework-setup.md §D note) so both the list (iteration 2) and
 * the map detail sheet (iteration 4) share one `external_open_url`/`external_open_error` banner,
 * regardless of which screen is on top.
 */
@Composable
fun ExternalLinkBanner(state: ExternalLinkState, modifier: Modifier = Modifier) {
    when (state) {
        is ExternalLinkState.Captured -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Surface(
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = state.url,
                    modifier = Modifier.testTag("external_open_url").padding(8.dp),
                )
            }
        }
        ExternalLinkState.Error -> Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Surface(
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = "Couldn't open this video externally.",
                    modifier = Modifier.testTag("external_open_error").padding(8.dp),
                )
            }
        }
        ExternalLinkState.Idle -> Unit
    }
}
