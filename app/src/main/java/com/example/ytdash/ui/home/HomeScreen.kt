package com.example.ytdash.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.ytdash.domain.model.Video
import com.example.ytdash.ui.common.ErrorView
import com.example.ytdash.ui.common.LoadingView

private enum class Panel { FILTER, SORT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onOpenMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activePanel by remember { mutableStateOf<Panel?>(null) }

    Scaffold(
        modifier = Modifier.testTag("screen_home"),
        topBar = {
            TopAppBar(
                title = {
                    val count = (uiState as? HomeUiState.Content)?.totalCount ?: 0
                    Text("Videos ($count)", modifier = Modifier.testTag("video_count"))
                },
                actions = {
                    IconButton(
                        modifier = Modifier.testTag("refresh_control"),
                        onClick = { viewModel.refresh() },
                    ) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
                    IconButton(
                        modifier = Modifier.testTag("filter_button"),
                        onClick = { activePanel = Panel.FILTER },
                    ) { Icon(Icons.Default.FilterList, contentDescription = "Filter") }
                    IconButton(
                        modifier = Modifier.testTag("sort_button"),
                        onClick = { activePanel = Panel.SORT },
                    ) { Icon(Icons.Default.Sort, contentDescription = "Sort") }
                    IconButton(
                        modifier = Modifier.testTag("map_nav_button"),
                        onClick = onOpenMap,
                    ) { Icon(Icons.Default.Map, contentDescription = "Map") }
                    IconButton(
                        modifier = Modifier.testTag("logout_button"),
                        onClick = { viewModel.logout(); onLogout() },
                    ) { Icon(Icons.Default.Logout, contentDescription = "Logout") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val panel = activePanel
            val state = uiState
            when {
                panel == Panel.FILTER && state is HomeUiState.Content -> FilterSheet(
                    categories = state.categories,
                    current = state.filter.category,
                    onApply = { category -> viewModel.applyFilter(category); activePanel = null },
                    onDismiss = { activePanel = null },
                )
                panel == Panel.SORT && state is HomeUiState.Content -> SortSheet(
                    current = state.sort,
                    onApply = { sort -> viewModel.applySort(sort); activePanel = null },
                    onDismiss = { activePanel = null },
                )
                state is HomeUiState.Loading -> LoadingView()
                state is HomeUiState.Error -> ErrorView(message = state.message, onRetry = viewModel::refresh)
                state is HomeUiState.Content -> VideoListContent(state.videos, onOpen = viewModel::openVideo)
            }
        }
    }
}

@Composable
private fun VideoListContent(videos: List<Video>, onOpen: (Video) -> Unit) {
    if (videos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos to show.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().testTag("video_list")) {
        items(videos, key = { it.id }) { video ->
            VideoRow(video = video, onClick = { onOpen(video) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun VideoRow(video: Video, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(Modifier.padding(start = 12.dp)) {
            // Compose merges unmerged semantics differently than RN/Flutter — the id must sit on
            // the title Text node itself (cross-framework-setup.md §D.4), not the clickable row.
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("video_list_item"),
            )
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
        }
    }
}
