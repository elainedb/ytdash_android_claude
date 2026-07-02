package com.example.ytdash.ui

import android.preference.PreferenceManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ytdash.data.model.Video
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(ui: AppUiState, viewModel: AppViewModel) {
    val markers = ui.mapMarkers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(Tags.SCREEN_MAP),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.backToHome() }) { Text("Back") }
            Text("Map (${markers.size} located)", style = MaterialTheme.typography.titleMedium)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Native, ACCESSIBLE marker affordance (constitution §5): osmdroid pins are
                // Canvas-drawn and unreachable by Maestro, so each located video also gets a chip.
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(markers, key = { it.id }) { video ->
                        Button(
                            onClick = { viewModel.selectMarker(video) },
                            modifier = Modifier.testTag(Tags.MAP_MARKER),
                        ) {
                            Text(
                                text = video.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                OsmMap(markers = markers, modifier = Modifier.fillMaxSize())
            }

            if (ui.selectedVideo != null) {
                DetailBottomSheet(
                    video = ui.selectedVideo,
                    onOpen = { viewModel.openInYouTube(ui.selectedVideo.youtubeUrl) },
                    onDismiss = { viewModel.dismissSheet() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun DetailBottomSheet(
    video: Video,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Inline Surface overlay (NOT ModalBottomSheet) so its testTags stay in the main composition.
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag(Tags.DETAIL_BOTTOM_SHEET),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(video.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            // Text is EXACTLY the watch URL so AC-MAP-03 can copy it and compare to external_open_url.
            Text(
                text = video.youtubeUrl,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag(Tags.DETAIL_VIDEO_URL),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen, modifier = Modifier.testTag(Tags.DETAIL_OPEN_YOUTUBE_BUTTON)) {
                    Text("Open in YouTube")
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}

@Composable
private fun OsmMap(markers: List<Video>, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // OSM requires a User-Agent or tile fetches 403 (cross-framework-setup §C).
            Configuration.getInstance().apply {
                load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
                userAgentValue = ctx.packageName
            }
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(3.0)
                onResume()
            }
        },
        update = { map ->
            map.overlays.clear()
            markers.forEach { v ->
                if (v.lat != null && v.lng != null) {
                    val marker = Marker(map).apply {
                        position = GeoPoint(v.lat, v.lng)
                        title = v.title
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(marker)
                }
            }
            markers.firstOrNull { it.lat != null && it.lng != null }?.let {
                map.controller.setCenter(GeoPoint(it.lat!!, it.lng!!))
            }
            map.invalidate()
        },
    )
}
