package dev.elainedb.ytdash_android_claude.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_claude.model.Video
import dev.elainedb.ytdash_android_claude.viewmodel.FilterOptions
import dev.elainedb.ytdash_android_claude.viewmodel.SortOption
import dev.elainedb.ytdash_android_claude.viewmodel.VideoListUiState
import dev.elainedb.ytdash_android_claude.viewmodel.VideoListViewModel

@Composable
fun VideoListScreen(
    viewModel: VideoListViewModel,
    onViewMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val availableCountries by viewModel.availableCountries.collectAsState()
    val availableChannels by viewModel.availableChannels.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.refreshVideos() }) {
                Text("Refresh")
            }
            Button(onClick = onViewMap) {
                Text("View Map")
            }
            Button(onClick = { showFilterDialog = true }) {
                Text("Filter")
            }
            Button(onClick = { showSortDialog = true }) {
                Text("Sort")
            }
        }

        // Video count display
        if (uiState is VideoListUiState.Success) {
            val state = uiState as VideoListUiState.Success
            val showing = state.videos.size
            val total = state.totalCount
            if (showing != total) {
                Text(
                    text = "Showing $showing of $total videos",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        when (val state = uiState) {
            is VideoListUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading videos...")
                }
            }
            is VideoListUiState.Empty -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No videos found")
                }
            }
            is VideoListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.videos, key = { it.id }) { video ->
                        VideoItem(video = video)
                    }
                }
            }
            is VideoListUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error: ${state.message}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refreshVideos() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentFilter = filterOptions,
            availableChannels = availableChannels,
            availableCountries = availableCountries,
            onApply = { filter ->
                viewModel.applyFilter(filter)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentSort = sortOption,
            onApply = { sort ->
                viewModel.applySorting(sort)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoItem(video: Video) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clickable { openVideo(context, video.id) }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = video.channelName,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = video.publishedAt.take(10),
                style = MaterialTheme.typography.bodySmall
            )

            // Tags
            if (video.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    video.tags.take(5).forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Location
            if (video.locationCity != null || video.locationCountry != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val locationParts = listOfNotNull(video.locationCity, video.locationCountry)
                Text(
                    text = locationParts.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall
                )
                if (video.locationLatitude != null && video.locationLongitude != null) {
                    Text(
                        text = "(${video.locationLatitude}, ${video.locationLongitude})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Recording date
            if (video.recordingDate != null) {
                Text(
                    text = "Recorded: ${video.recordingDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

fun openVideo(context: Context, videoId: String) {
    try {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        context.startActivity(appIntent)
    } catch (e: Exception) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        )
        context.startActivity(webIntent)
    }
}
