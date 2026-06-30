package com.example.ytdash.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ytdash.domain.Video
import com.example.ytdash.ui.common.tag
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    located: List<Video>,
    selected: Video?,
    onBack: () -> Unit,
    onMarker: (Video) -> Unit,
    onDismissSheet: () -> Unit,
    onOpen: (Video) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().tag("screen_map"),
        topBar = {
            TopAppBar(
                title = { Text("Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Real OpenStreetMap map. Canvas-drawn markers are NOT reachable by a black-box driver
            // (constitution §5) — the accessible affordance is the native chip row below.
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(3.5)
                        located.firstOrNull()?.let {
                            controller.setCenter(GeoPoint(it.lat!!, it.lng!!))
                        }
                        located.forEach { v ->
                            overlays.add(
                                Marker(this).apply {
                                    position = GeoPoint(v.lat!!, v.lng!!)
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    title = v.title
                                },
                            )
                        }
                        invalidate()
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Accessible marker affordance: one chip per located video, each carrying map_marker.
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(8.dp),
            ) {
                Text(
                    "Markers (${located.size})",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    located.forEach { video ->
                        AssistChip(
                            onClick = { onMarker(video) },
                            label = { Text(video.title) },
                            modifier = Modifier.tag("map_marker"),
                        )
                    }
                }
            }

            if (selected != null) {
                DetailSheet(
                    video = selected,
                    onDismiss = onDismissSheet,
                    onOpen = onOpen,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun DetailSheet(
    video: Video,
    onDismiss: () -> Unit,
    onOpen: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Inline Surface (NOT ModalBottomSheet): a ModalBottomSheet renders in a separate composition
    // window where testTagsAsResourceId does not apply (constitution §5a). This keeps the sheet's
    // ids reachable by Maestro.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tag("detail_bottom_sheet"),
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            // Text == the exact watch URL, so the harness can compare it to the opened URL.
            Text(
                text = video.youtubeUrl,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 8.dp).tag("detail_video_url"),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onOpen(video) },
                    modifier = Modifier.tag("detail_open_youtube_button"),
                ) {
                    Text("Open in YouTube")
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}
