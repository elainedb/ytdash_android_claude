package dev.elainedb.ytdash_android_claude.video.presentation

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_claude.video.domain.model.Video

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
    val availableCountries by viewModel.availableCountries.collectAsState()
    val availableChannels by viewModel.availableChannels.collectAsState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YTDash") },
                actions = {
                    Button(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.loadVideos(forceRefresh = true) }) {
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

            when (val state = uiState) {
                is VideoListUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is VideoListUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No videos found")
                    }
                }
                is VideoListUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadVideos(forceRefresh = true) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is VideoListUiState.Success -> {
                    val hasFilters = filterOptions.channelName != null || filterOptions.country != null
                    if (hasFilters) {
                        Text(
                            text = "Showing ${state.videos.size} of ${state.totalCount} videos",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.videos, key = { it.id }) { video ->
                            VideoItem(video = video)
                        }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openVideo(context, video.id) }
            .padding(12.dp)
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row {
            Text(
                text = video.channelName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = video.publishedAt.take(10),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (video.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                video.tags.take(5).forEach { tag ->
                    AssistChip(
                        onClick = {},
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (video.locationCity != null || video.locationCountry != null) {
            Spacer(modifier = Modifier.height(4.dp))
            val locationParts = listOfNotNull(video.locationCity, video.locationCountry)
            val coordsText = if (video.locationLatitude != null && video.locationLongitude != null) {
                " (${String.format("%.4f", video.locationLatitude)}, ${String.format("%.4f", video.locationLongitude)})"
            } else ""
            Text(
                text = locationParts.joinToString(", ") + coordsText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (video.recordingDate != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Recorded: ${video.recordingDate.take(10)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openVideo(context: Context, videoId: String) {
    try {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        context.startActivity(appIntent)
    } catch (_: Exception) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        )
        context.startActivity(webIntent)
    }
}
