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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ytdash.TestTags
import com.example.ytdash.data.model.Video
import com.example.ytdash.ui.testTagAs
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onOpenVideo: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Video?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTagAs(TestTags.SCREEN_MAP),
    ) {
        when (val s = state) {
            is MapUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.testTagAs(TestTags.LOADING_INDICATOR))
            }

            is MapUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No located videos.")
            }

            is MapUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTagAs(TestTags.ERROR_VIEW),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Couldn't load the map", style = MaterialTheme.typography.titleMedium)
                Text(s.message, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { viewModel.load() },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .testTagAs(TestTags.ERROR_RETRY_BUTTON),
                ) { Text("Retry") }
            }

            is MapUiState.Content -> MapContent(
                located = s.located,
                onSelect = { selected = it },
            )
        }

        // Back control
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }

        // ---- Inline detail bottom sheet (NOT ModalBottomSheet — keep ids in the main composition,
        // constitution §5a). Shown when a marker is selected. ----
        selected?.let { video ->
            DetailSheet(
                video = video,
                onOpenVideo = onOpenVideo,
                onDismiss = { selected = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun MapContent(located: List<Video>, onSelect: (Video) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        // The OpenStreetMap map (osmdroid). Markers drawn here are Canvas-based and NOT reachable
        // by Maestro — the accessible affordance is the chip row below (constitution §5).
        OsmMap(
            located = located,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        // Native, accessible marker affordance: one map_marker chip per located video.
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                located.forEach { video ->
                    AssistChip(
                        onClick = { onSelect(video) },
                        label = {
                            Text(
                                text = video.title,
                                maxLines = 1,
                            )
                        },
                        modifier = Modifier.testTagAs(TestTags.MAP_MARKER),
                    )
                }
            }
        }
    }
}

@Composable
private fun OsmMap(located: List<Video>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Required or OSM tile fetches return 403 (cross-framework-setup §C).
            Configuration.getInstance().userAgentValue = ctx.packageName
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(3.0)
            }
        },
        update = { map ->
            map.overlays.clear()
            located.forEach { video ->
                if (video.lat != null && video.lng != null) {
                    val marker = Marker(map).apply {
                        position = GeoPoint(video.lat, video.lng)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = video.title
                    }
                    map.overlays.add(marker)
                }
            }
            located.firstOrNull { it.lat != null && it.lng != null }?.let {
                map.controller.setCenter(GeoPoint(it.lat!!, it.lng!!))
            }
            map.invalidate()
        },
    )
}

@Composable
private fun DetailSheet(
    video: Video,
    onOpenVideo: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTagAs(TestTags.DETAIL_BOTTOM_SHEET),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(video.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
            )
            // The exact watch URL, exposed so the harness can verify the opened URL matches.
            Text(
                text = video.youtubeUrl,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTagAs(TestTags.DETAIL_VIDEO_URL),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onOpenVideo(video.youtubeUrl) },
                    modifier = Modifier.testTagAs(TestTags.DETAIL_OPEN_YOUTUBE_BUTTON),
                ) { Text("Open in YouTube") }
                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}
