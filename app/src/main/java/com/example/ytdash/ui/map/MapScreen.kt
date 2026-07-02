package com.example.ytdash.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ytdash.ui.detail.DetailSheet
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val located by viewModel.locatedVideos.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.testTag("screen_map"),
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
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(3.0)
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    located.forEach { video ->
                        val lat = video.lat ?: return@forEach
                        val lng = video.lng ?: return@forEach
                        val marker = Marker(mapView)
                        marker.position = GeoPoint(lat, lng)
                        marker.title = video.title
                        mapView.overlays.add(marker)
                    }
                    located.firstOrNull()?.let { first ->
                        mapView.controller.setCenter(GeoPoint(first.lat!!, first.lng!!))
                    }
                    mapView.invalidate()
                },
            )

            // Reachable affordance (constitution §5) — osmdroid draws pins on a Canvas with no
            // accessibility nodes; this native chip row is what Maestro actually taps.
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
            ) {
                items(located, key = { it.id }) { video ->
                    AssistChip(
                        modifier = Modifier.testTag("map_marker").padding(end = 8.dp),
                        onClick = { viewModel.select(video) },
                        label = { Text(video.title, maxLines = 1) },
                    )
                }
            }

            selected?.let { video ->
                DetailSheet(
                    video = video,
                    onOpen = { viewModel.openSelected() },
                    onDismiss = { viewModel.clearSelection() },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
