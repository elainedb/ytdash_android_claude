package com.example.ytdash.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ytdash.TestTags
import com.example.ytdash.data.model.Video
import com.example.ytdash.domain.SortOrder
import com.example.ytdash.ui.testTagAs

private enum class Panel { NONE, FILTER, SORT }

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenVideo: (String) -> Unit,
    onNavigateToMap: () -> Unit,
    onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var panel by remember { mutableStateOf(Panel.NONE) }

    val totalCount = (state as? HomeUiState.Content)?.all?.size ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTagAs(TestTags.SCREEN_HOME),
    ) {
        // ---- Header: title + total loaded count ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Videos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "  ·  $totalCount videos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTagAs(TestTags.VIDEO_COUNT),
            )
        }

        // ---- Actions ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { panel = if (panel == Panel.FILTER) Panel.NONE else Panel.FILTER },
                modifier = Modifier.testTagAs(TestTags.FILTER_BUTTON),
            ) { Icon(Icons.Filled.FilterList, contentDescription = "Filter") }

            IconButton(
                onClick = { panel = if (panel == Panel.SORT) Panel.NONE else Panel.SORT },
                modifier = Modifier.testTagAs(TestTags.SORT_BUTTON),
            ) { Icon(Icons.Filled.Sort, contentDescription = "Sort") }

            IconButton(
                onClick = { viewModel.load(forceRefresh = true) },
                modifier = Modifier.testTagAs(TestTags.REFRESH_CONTROL),
            ) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }

            IconButton(
                onClick = onNavigateToMap,
                modifier = Modifier.testTagAs(TestTags.MAP_NAV_BUTTON),
            ) { Icon(Icons.Filled.Map, contentDescription = "Map") }

            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTagAs(TestTags.LOGOUT_BUTTON),
            ) { Icon(Icons.Filled.Logout, contentDescription = "Log out") }
        }

        Divider()

        // ---- Body ----
        Box(modifier = Modifier.fillMaxSize()) {
            when (panel) {
                Panel.FILTER -> FilterPanel(
                    categories = (state as? HomeUiState.Content)?.categories ?: emptyList(),
                    active = (state as? HomeUiState.Content)?.activeCategory,
                    onSelect = {
                        viewModel.setCategory(it)
                        panel = Panel.NONE
                    },
                )

                Panel.SORT -> SortPanel(
                    active = (state as? HomeUiState.Content)?.sort ?: SortOrder.DATE_DESC,
                    onSelect = {
                        viewModel.setSort(it)
                        panel = Panel.NONE
                    },
                )

                Panel.NONE -> when (val s = state) {
                    is HomeUiState.Loading -> LoadingBody()
                    is HomeUiState.Empty -> EmptyBody()
                    is HomeUiState.Error -> ErrorBody(s.message) { viewModel.load(forceRefresh = true) }
                    is HomeUiState.Content -> VideoList(s.visible, onOpenVideo)
                }
            }
        }
    }
}

@Composable
private fun VideoList(videos: List<Video>, onOpenVideo: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTagAs(TestTags.VIDEO_LIST),
    ) {
        items(videos, key = { it.id }) { video ->
            VideoRow(video, onOpenVideo)
            Divider()
        }
    }
}

@Composable
private fun VideoRow(video: Video, onOpenVideo: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenVideo(video.youtubeUrl) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(width = 96.dp, height = 54.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            // Per cross-framework-setup §D.4: the list-item id must sit on the TITLE Text so the
            // id and the title text resolve to the SAME node (AC-SORT-01 / AC-LIST-01).
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTagAs(TestTags.VIDEO_LIST_ITEM),
            )
            Text(
                text = video.category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FilterPanel(
    categories: List<String>,
    active: String?,
    onSelect: (String?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Filter by category", style = MaterialTheme.typography.titleMedium)
        PanelOption(text = "All", selected = active == null) { onSelect(null) }
        categories.forEach { category ->
            PanelOption(text = category, selected = active.equals(category, ignoreCase = true)) {
                onSelect(category)
            }
        }
    }
}

@Composable
private fun SortPanel(active: SortOrder, onSelect: (SortOrder) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sort by", style = MaterialTheme.typography.titleMedium)
        SortOrder.entries.forEach { order ->
            PanelOption(text = order.label, selected = order == active) { onSelect(order) }
        }
    }
}

@Composable
private fun PanelOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = if (selected) "✓ $text" else text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

@Composable
private fun LoadingBody() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.testTagAs(TestTags.LOADING_INDICATOR))
    }
}

@Composable
private fun EmptyBody() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No videos to show.")
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTagAs(TestTags.ERROR_VIEW),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Couldn't load videos", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = 16.dp)
                .testTagAs(TestTags.ERROR_RETRY_BUTTON),
        ) { Text("Retry") }
    }
}
