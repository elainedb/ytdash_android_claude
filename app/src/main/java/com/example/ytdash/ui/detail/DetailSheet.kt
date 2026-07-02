package com.example.ytdash.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ytdash.domain.model.Video

/**
 * Inline `Surface`, NOT `ModalBottomSheet` — a modal sheet is a separate composition window
 * where the root `testTagsAsResourceId` flag doesn't reach (constitution §5a). Kept in the main
 * composition so `detail_bottom_sheet` / `detail_open_youtube_button` stay reachable.
 */
@Composable
fun DetailSheet(video: Video, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("detail_bottom_sheet"),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleLarge)
            Text(video.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Text(
                text = video.youtubeUrl,
                modifier = Modifier.testTag("detail_video_url").padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Close") }
                Button(
                    modifier = Modifier.testTag("detail_open_youtube_button"),
                    onClick = onOpen,
                ) { Text("Open in YouTube") }
            }
        }
    }
}
