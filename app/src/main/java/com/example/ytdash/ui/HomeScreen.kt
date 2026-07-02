package com.example.ytdash.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ytdash.data.model.Video
import com.example.ytdash.domain.SortMode

@Composable
fun HomeScreen(ui: AppUiState, viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(Tags.SCREEN_HOME),
    ) {
        // ---- Header: title with the total count + refresh / logout ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Videos: ${ui.totalCount}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag(Tags.VIDEO_COUNT),
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { viewModel.loadVideos() }, modifier = Modifier.testTag(Tags.REFRESH_CONTROL)) {
                Text("Refresh")
            }
            TextButton(onClick = { viewModel.logout() }, modifier = Modifier.testTag(Tags.LOGOUT_BUTTON)) {
                Text("Logout")
            }
        }

        // ---- Toolbar: filter / sort / map ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { viewModel.openFilterPanel() }, modifier = Modifier.testTag(Tags.FILTER_BUTTON)) {
                Text("Filter")
            }
            Button(onClick = { viewModel.openSortPanel() }, modifier = Modifier.testTag(Tags.SORT_BUTTON)) {
                Text("Sort")
            }
            Button(onClick = { viewModel.openMap() }, modifier = Modifier.testTag(Tags.MAP_NAV_BUTTON)) {
                Text("Map")
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---- Content: a panel REPLACES the list while open (avoids text collisions for Maestro) ----
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                ui.filterPanelOpen -> FilterPanel(ui, viewModel)
                ui.sortPanelOpen -> SortPanel(ui, viewModel)
                ui.listPhase == ListPhase.LOADING ->
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag(Tags.LOADING_INDICATOR),
                    )
                ui.listPhase == ListPhase.ERROR -> ErrorView(onRetry = { viewModel.retry() })
                ui.listPhase == ListPhase.EMPTY ->
                    Text(
                        text = "No videos to show.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                else -> VideoList(ui.videos, onOpen = { viewModel.openInYouTube(it.youtubeUrl) })
            }
        }
    }
}

@Composable
private fun VideoList(videos: List<Video>, onOpen: (Video) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(Tags.VIDEO_LIST),
    ) {
        items(videos, key = { it.id }) { video ->
            VideoRow(video, onClick = { onOpen(video) })
            Divider()
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(width = 96.dp, height = 54.dp)
                .background(Color.LightGray),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Tag the TITLE text directly (Compose reads the unmerged tree): id + text on the SAME
            // node so AC-SORT-01 (id:video_list_item text:"ZZZ…") resolves. Tapping it still fires
            // the row's clickable, so AC-LIST-03 keeps working.
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag(Tags.VIDEO_LIST_ITEM),
            )
            Text(
                text = video.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
private fun ErrorView(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(Tags.ERROR_VIEW),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Couldn't load videos.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, modifier = Modifier.testTag(Tags.ERROR_RETRY_BUTTON)) {
            Text("Retry")
        }
    }
}

@Composable
private fun FilterPanel(ui: AppUiState, viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Filter by category", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OptionRow(label = "All", selected = ui.selectedCategory == null) {
            viewModel.selectCategory(null)
        }
        ui.categories.forEach { category ->
            OptionRow(label = category, selected = ui.selectedCategory == category) {
                viewModel.selectCategory(category)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.applyFilter() }, modifier = Modifier.testTag(Tags.FILTER_APPLY_BUTTON)) {
            Text("Apply")
        }
    }
}

@Composable
private fun SortPanel(ui: AppUiState, viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Sort by", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        SortMode.entries.forEach { mode ->
            OptionRow(label = mode.label, selected = ui.sortMode == mode) {
                viewModel.selectSort(mode)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.applySort() }, modifier = Modifier.testTag(Tags.SORT_APPLY_BUTTON)) {
            Text("Apply")
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
