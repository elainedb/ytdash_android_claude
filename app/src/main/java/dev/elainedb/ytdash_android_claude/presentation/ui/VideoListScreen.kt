package dev.elainedb.ytdash_android_claude.presentation.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_claude.domain.model.Video
import dev.elainedb.ytdash_android_claude.presentation.viewmodel.FilterOptions
import dev.elainedb.ytdash_android_claude.presentation.viewmodel.SortOption
import dev.elainedb.ytdash_android_claude.presentation.viewmodel.VideoListUiState
import dev.elainedb.ytdash_android_claude.presentation.viewmodel.VideoListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    viewModel: VideoListViewModel,
    onLogout: () -> Unit,
    onViewMap: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterOptions by viewModel.filterOptions.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val availableChannels by viewModel.availableChannels.collectAsState()
    val availableCountries by viewModel.availableCountries.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        FilterDialog(
            currentFilter = filterOptions,
            availableChannels = availableChannels,
            availableCountries = availableCountries,
            onApply = {
                viewModel.applyFilter(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentSort = sortOption,
            onApply = {
                viewModel.applySorting(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YTDash") },
                actions = {
                    TextButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Control buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { viewModel.refreshVideos() }, modifier = Modifier.weight(1f)) {
                    Text("Refresh")
                }
                Button(onClick = onViewMap, modifier = Modifier.weight(1f)) {
                    Text("View Map")
                }
                Button(onClick = { showFilterDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("Filter")
                }
                Button(onClick = { showSortDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("Sort")
                }
            }

            // Video count display
            if (uiState is VideoListUiState.Success) {
                val state = uiState as VideoListUiState.Success
                val hasFilters = filterOptions.channelName != null || filterOptions.country != null
                if (hasFilters) {
                    Text(
                        text = "Showing ${state.videos.size} of ${state.totalCount} videos",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            when (val state = uiState) {
                is VideoListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is VideoListUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No videos found")
                    }
                }
                is VideoListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshVideos() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is VideoListUiState.Success -> {
                    val context = LocalContext.current
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.videos, key = { it.id }) { video ->
                            VideoItem(video = video, onClick = { openVideo(context, video.id) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoItem(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = video.channelName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = video.publishedAt.take(10),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tags
            if (video.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    video.tags.take(5).forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Location
            if (video.locationCity != null || video.locationCountry != null) {
                val locationParts = listOfNotNull(video.locationCity, video.locationCountry)
                Text(
                    text = locationParts.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (video.locationLatitude != null && video.locationLongitude != null) {
                    Text(
                        text = "GPS: ${video.locationLatitude}, ${video.locationLongitude}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Recording date
            if (video.recordingDate != null) {
                Text(
                    text = "Recorded: ${video.recordingDate.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun openVideo(context: Context, videoId: String) {
    try {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        context.startActivity(appIntent)
    } catch (_: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
        context.startActivity(webIntent)
    }
}
