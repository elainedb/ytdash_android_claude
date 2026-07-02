package com.example.ytdash.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ytdash.core.link.ExternalLinkViewModel
import com.example.ytdash.domain.model.Video
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    externalLinkViewModel: ExternalLinkViewModel,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.testTag("screen_map"),
        topBar = { TopAppBar(title = { Text("Map") }) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                OsmMapView(
                    locatedVideos = state.locatedVideos,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )

                // Native, accessible marker affordance (constitution §5). osmdroid renders
                // markers on a Canvas — no accessibility nodes — so each located video also gets
                // a real Compose chip here that Maestro can select by `map_marker` and tap.
                MarkerChipRow(
                    videos = state.locatedVideos,
                    onSelect = { viewModel.selectVideo(it.id) },
                )
            }

            state.selectedVideo?.let { video ->
                DetailBottomSheet(
                    video = video,
                    onOpenYoutube = { externalLinkViewModel.openVideo(context, video.youtubeUrl) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun OsmMapView(locatedVideos: List<Video>, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            Configuration.getInstance().apply {
                userAgentValue = context.packageName
                osmdroidTileCache = context.cacheDir
            }
            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(2.0)
                controller.setCenter(GeoPoint(20.0, 0.0))
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            locatedVideos.forEach { video ->
                val location = video.location ?: return@forEach
                val marker = Marker(mapView)
                marker.position = GeoPoint(location.lat, location.lng)
                marker.title = video.title
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
            if (locatedVideos.isNotEmpty()) {
                val first = locatedVideos.first().location!!
                mapView.controller.setCenter(GeoPoint(first.lat, first.lng))
                mapView.controller.setZoom(4.0)
            }
        },
    )
}

@Composable
private fun MarkerChipRow(videos: List<Video>, onSelect: (Video) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(videos, key = { it.id }) { video ->
            AssistChip(
                onClick = { onSelect(video) },
                label = {
                    Text(
                        text = video.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.testTag("map_marker"),
            )
        }
    }
}

@Composable
private fun DetailBottomSheet(video: Video, onOpenYoutube: () -> Unit, modifier: Modifier = Modifier) {
    // An inline Surface, not a ModalBottomSheet — Compose popups/dialogs render in a separate
    // composition window where the root `testTagsAsResourceId` doesn't reach (constitution §5a).
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("detail_bottom_sheet"),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = video.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = video.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = video.youtubeUrl,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("detail_video_url"),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onOpenYoutube, modifier = Modifier.testTag("detail_open_youtube_button")) {
                Text("Open in YouTube")
            }
        }
    }
}
