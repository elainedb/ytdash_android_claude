package com.example.ytdash.presentation.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.example.ytdash.core.link.ExternalLinkViewModel
import com.example.ytdash.domain.model.SortOption
import com.example.ytdash.domain.model.Video
import com.example.ytdash.presentation.common.ErrorView
import com.example.ytdash.presentation.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    externalLinkViewModel: ExternalLinkViewModel,
    onLogout: () -> Unit,
    onNavigateToMap: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.testTag("screen_home"),
        topBar = {
            val totalCount = (state as? HomeUiState.Content)?.totalCount ?: 0
            TopAppBar(
                title = {
                    Text(
                        text = "ytdash ($totalCount)",
                        modifier = Modifier.testTag("video_count"),
                    )
                },
                actions = {
                    TextButton(onClick = { viewModel.refresh() }, modifier = Modifier.testTag("refresh_control")) {
                        Text("Refresh")
                    }
                    TextButton(onClick = { viewModel.openFilterPanel() }, modifier = Modifier.testTag("filter_button")) {
                        Text("Filter")
                    }
                    TextButton(onClick = { viewModel.openSortPanel() }, modifier = Modifier.testTag("sort_button")) {
                        Text("Sort")
                    }
                    TextButton(onClick = onNavigateToMap, modifier = Modifier.testTag("map_nav_button")) {
                        Text("Map")
                    }
                    TextButton(
                        onClick = {
                            viewModel.logout()
                            onLogout()
                        },
                        modifier = Modifier.testTag("logout_button"),
                    ) {
                        Text("Logout")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                is HomeUiState.Loading -> LoadingView()
                is HomeUiState.Error -> ErrorView(message = current.message, onRetry = { viewModel.refresh() })
                is HomeUiState.Content -> when (current.panelMode) {
                    PanelMode.FILTER -> FilterPanel(
                        categories = current.availableCategories,
                        selected = current.filterCategory,
                        onSelect = { viewModel.selectFilter(it) },
                    )
                    PanelMode.SORT -> SortPanel(
                        selected = current.sortOption,
                        onSelect = { viewModel.selectSort(it) },
                    )
                    PanelMode.NONE -> VideoListBody(
                        videos = current.displayVideos,
                        onVideoClick = { video ->
                            externalLinkViewModel.openVideo(context, video.youtubeUrl)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoListBody(videos: List<Video>, onVideoClick: (Video) -> Unit) {
    if (videos.isEmpty()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No videos to show.",
                modifier = Modifier.padding(24.dp).testTag("empty_view"),
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("video_list")) {
        items(videos, key = { it.id }) { video ->
            VideoRow(video = video, onClick = { onVideoClick(video) })
        }
    }
}

@Composable
private fun VideoRow(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(width = 96.dp, height = 64.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                // The title Text carries `video_list_item` (not the Card/Row) — Compose's
                // unmerged semantics tree means a clickable row's own resource-id node has no
                // text; tagging the title directly is what lets Maestro read the row's title
                // via `id:video_list_item`. A tap here still bubbles to the Card's onClick.
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("video_list_item"),
                )
                Text(
                    text = video.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(onClick = {}, label = { Text(video.category) })
            }
        }
    }
}

@Composable
private fun FilterPanel(categories: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().testTag("filter_panel").padding(16.dp)) {
        Text(text = "Filter by category", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "All",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(null) }
                .padding(vertical = 12.dp),
        )
        categories.forEach { category ->
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(category) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SortPanel(selected: SortOption?, onSelect: (SortOption) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().testTag("sort_panel").padding(16.dp)) {
        Text(text = "Sort by", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        SortOption.entries.forEach { option ->
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}
