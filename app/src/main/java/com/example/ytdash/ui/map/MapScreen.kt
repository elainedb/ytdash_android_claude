package com.example.ytdash.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ytdash.domain.model.Video
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    onOpenExternalLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val locatedVideos by viewModel.locatedVideos.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedVideoId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = modifier.fillMaxSize().testTag("screen_map")) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                OsmMapView(videos = locatedVideos)
            }
            // Map §5 (constitution): osmdroid draws markers on a Canvas — no accessibility nodes.
            // This chip row is the native, accessible affordance the harness drives; it selects
            // the SAME video the corresponding rendered pin represents (see OsmMapView ordering).
            LazyRow(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                items(locatedVideos, key = { it.id }) { video ->
                    AssistChip(
                        onClick = { viewModel.select(video.id) },
                        label = { Text(video.title) },
                        modifier = Modifier.padding(end = 8.dp).testTag("map_marker"),
                    )
                }
            }
        }

        val selected = locatedVideos.firstOrNull { it.id == selectedId }
        if (selected != null) {
            DetailSheet(
                video = selected,
                onOpenYoutube = onOpenExternalLink,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun OsmMapView(videos: List<Video>, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(3.0)
                controller.setCenter(GeoPoint(20.0, 0.0))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            videos.forEach { video ->
                val lat = video.lat
                val lng = video.lng
                if (lat != null && lng != null) {
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(lat, lng)
                    marker.title = video.title
                    mapView.overlays.add(marker)
                }
            }
            mapView.invalidate()
        },
    )
}
