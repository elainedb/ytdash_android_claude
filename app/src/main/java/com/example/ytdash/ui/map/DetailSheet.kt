package com.example.ytdash.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ytdash.domain.model.Video

/**
 * Deliberately an inline `Surface`, NOT `ModalBottomSheet` (constitution §5a) — a modal bottom
 * sheet renders in a separate composition window where the root's `testTagsAsResourceId` doesn't
 * reach its `testTag`s.
 */
@Composable
fun DetailSheet(video: Video, onOpenYoutube: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("detail_bottom_sheet"),
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(text = video.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = video.youtubeUrl,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("detail_video_url").padding(top = 4.dp),
            )
            Button(
                onClick = { onOpenYoutube(video.youtubeUrl) },
                modifier = Modifier.padding(top = 12.dp).testTag("detail_open_youtube_button"),
            ) { Text("Open in YouTube") }
        }
    }
}
