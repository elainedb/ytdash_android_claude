package com.example.ytdash.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ytdash.core.ExternalLinkLauncher

/**
 * App-root banner fed by both the list (iteration 2) and the map detail sheet (iteration 4)
 * via one [ExternalLinkLauncher] — single `external_open_url`/`external_open_error` surface
 * (cross-framework-setup.md: "lift captured/external_open_url to the app root").
 */
@Composable
fun ExternalLinkBanner(launcher: ExternalLinkLauncher, modifier: Modifier = Modifier) {
    val url = launcher.capturedUrl.value
    val error = launcher.lastError.value
    if (url != null) {
        Text(
            text = url,
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFDFF5E1))
                .testTag("external_open_url")
                .clickable { launcher.dismiss() }
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else if (error) {
        Text(
            text = "Couldn't open the external link.",
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFFE0E0))
                .testTag("external_open_error")
                .clickable { launcher.dismiss() }
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
