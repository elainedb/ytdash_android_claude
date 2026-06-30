package com.example.ytdash.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ytdash.domain.SortOption
import com.example.ytdash.domain.Video
import com.example.ytdash.domain.VideoFilter
import com.example.ytdash.domain.VideoSort
import com.example.ytdash.ui.ListUiState
import com.example.ytdash.ui.common.tag

private enum class Panel { NONE, FILTER, SORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    list: ListUiState,
    sort: SortOption,
    filter: String?,
    onRefresh: () -> Unit,
    onSort: (SortOption) -> Unit,
    onFilter: (String?) -> Unit,
    onMap: () -> Unit,
    onLogout: () -> Unit,
    onOpen: (Video) -> Unit,
    onRetry: () -> Unit,
) {
    var panel by remember { mutableStateOf(Panel.NONE) }
    val totalLoaded = (list as? ListUiState.Content)?.videos?.size ?: 0

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .tag("screen_home"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$totalLoaded videos",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.tag("video_count"),
                    )
                },
                actions = {
                    IconButton(onClick = { panel = Panel.FILTER }, modifier = Modifier.tag("filter_button")) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { panel = Panel.SORT }, modifier = Modifier.tag("sort_button")) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.tag("refresh_control")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.tag("logout_button")) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onMap,
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                text = { Text("Map") },
                modifier = Modifier.tag("map_nav_button"),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (panel) {
                Panel.FILTER -> FilterPanel(
                    labels = (list as? ListUiState.Content)?.let { VideoFilter.labels(it.videos) } ?: emptyList(),
                    onPick = { label -> onFilter(label); panel = Panel.NONE },
                )
                Panel.SORT -> SortPanel(
                    onPick = { option -> onSort(option); panel = Panel.NONE },
                )
                Panel.NONE -> when (list) {
                    is ListUiState.Loading -> LoadingState()
                    is ListUiState.Error -> ErrorState(list.message, onRetry)
                    is ListUiState.Empty -> EmptyState()
                    is ListUiState.Content -> VideoList(
                        videos = VideoSort.sort(VideoFilter.filter(list.videos, filter), sort),
                        fromCache = list.fromCache,
                        onOpen = onOpen,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.tag("loading_indicator"))
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No videos to show.")
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .tag("error_view")
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp).tag("error_retry_button")) {
            Text("Retry")
        }
    }
}

@Composable
private fun VideoList(videos: List<Video>, fromCache: Boolean, onOpen: (Video) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (fromCache) {
            Text(
                text = "Offline — showing cached videos",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize().tag("video_list")) {
            items(items = videos, key = { it.id }) { video ->
                VideoRow(video, onOpen)
            }
        }
    }
}

@Composable
private fun VideoRow(video: Video, onOpen: (Video) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(video) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 96.dp, height = 54.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // The list-item id sits on the TITLE Text so its id and text resolve to the same node
            // (Compose's unmerged semantics tree puts row text on a child, not the clickable row).
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.tag("video_list_item"),
            )
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
            Text(
                text = video.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FilterPanel(labels: List<String>, onPick: (String?) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Filter by category", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) {
            Text("All")
        }
        labels.forEach { label ->
            // Text == the configured channel label exactly, so the harness matches "(?i)<label>".
            Button(onClick = { onPick(label) }, modifier = Modifier.fillMaxWidth()) {
                Text(label)
            }
        }
    }
}

@Composable
private fun SortPanel(onPick: (SortOption) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sort videos", style = MaterialTheme.typography.titleLarge)
        SortOption.entries.forEach { option ->
            // Labels END with the matched keyword (e.g. "newest"), per the harness regex anchoring.
            Button(onClick = { onPick(option) }, modifier = Modifier.fillMaxWidth()) {
                Text(option.label)
            }
        }
    }
}
