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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.ytdash.domain.model.Video
import com.example.ytdash.domain.usecase.SortOrder
import com.example.ytdash.ui.common.ErrorView
import com.example.ytdash.ui.common.LoadingIndicator

private enum class Panel { FILTER, SORT }

@Composable
fun HomeScreen(
    onNavigateMap: () -> Unit,
    onOpenExternalLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activePanel by remember { mutableStateOf<Panel?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var currentFilter by remember { mutableStateOf<String?>(null) }
    var currentSort by remember { mutableStateOf(SortOrder.DATE_DESC) }

    val countText = when (val state = uiState) {
        is HomeUiState.Content -> "Videos (${state.totalCount})"
        else -> "Videos"
    }

    // statusBarsPadding(): enableEdgeToEdge() draws content behind the status bar; without this,
    // the top row sits under the status bar's own touch-handling window and its taps never reach
    // this Compose tree at all (found via `adb shell input tap` at the reported coordinates doing
    // nothing, even for a bare clickable Box — not a Maestro or IconButton issue).
    Box(modifier = modifier.fillMaxSize().statusBarsPadding().testTag("screen_home")) {
        Column(Modifier.fillMaxSize()) {
            HomeTopBar(
                countText = countText,
                onRefresh = viewModel::refresh,
                onFilter = { activePanel = Panel.FILTER },
                onSort = { activePanel = Panel.SORT },
                onToggleMenu = { menuOpen = !menuOpen },
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (activePanel) {
                    Panel.FILTER -> {
                        val categories = (uiState as? HomeUiState.Content)?.categories.orEmpty()
                        FilterPanel(
                            categories = categories,
                            current = currentFilter,
                            onApply = {
                                currentFilter = it
                                viewModel.setFilter(it)
                                activePanel = null
                            },
                            onDismiss = { activePanel = null },
                        )
                    }
                    Panel.SORT -> SortPanel(
                        current = currentSort,
                        onApply = {
                            currentSort = it
                            viewModel.setSort(it)
                            activePanel = null
                        },
                        onDismiss = { activePanel = null },
                    )
                    null -> when (val state = uiState) {
                        HomeUiState.Loading -> LoadingIndicator()
                        is HomeUiState.Error -> ErrorView(message = state.message, onRetry = viewModel::retry)
                        is HomeUiState.Content -> VideoListContent(
                            videos = state.videos,
                            onItemClick = { video -> onOpenExternalLink(video.youtubeUrl) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onNavigateMap,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("map_nav_button"),
        ) {
            Icon(Icons.Default.Map, contentDescription = "Map")
        }

        if (menuOpen) {
            // Deliberately NOT androidx.compose.material3.DropdownMenu — that renders in a
            // separate composition window where the root's testTagsAsResourceId doesn't apply
            // (constitution §5a). This stays in the main composition.
            Box(Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 8.dp)) {
                Surface(shadowElevation = 8.dp, tonalElevation = 2.dp) {
                    Column {
                        TextButton(
                            onClick = {
                                menuOpen = false
                                viewModel.logout()
                            },
                            modifier = Modifier.testTag("logout_button"),
                        ) { Text("Logout") }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    countText: String,
    onRefresh: () -> Unit,
    onFilter: () -> Unit,
    onSort: () -> Unit,
    onToggleMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = countText,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).testTag("video_count"),
        )
        IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_control")) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
        IconButton(onClick = onFilter, modifier = Modifier.testTag("filter_button")) {
            Icon(Icons.Default.FilterList, contentDescription = "Filter")
        }
        IconButton(onClick = onSort, modifier = Modifier.testTag("sort_button")) {
            Icon(Icons.Default.Sort, contentDescription = "Sort")
        }
        IconButton(onClick = onToggleMenu, modifier = Modifier.testTag("overflow_menu_button")) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
    }
}

@Composable
private fun VideoListContent(
    videos: List<Video>,
    onItemClick: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) {
        Box(modifier.fillMaxSize().testTag("video_list"), contentAlignment = Alignment.Center) {
            Text("No videos yet.")
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize().testTag("video_list")) {
            items(videos, key = { it.id }) { video ->
                VideoRow(video = video, onClick = { onItemClick(video) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun VideoRow(video: Video, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(80.dp, 60.dp).background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            // Compose-specific gotcha (cross-framework-setup.md §D.4): Maestro reads the
            // UNMERGED semantics tree, so the id must sit on the title Text node itself, not the
            // clickable Row — a tap here still reaches the Row's clickable modifier.
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                modifier = Modifier.testTag("video_list_item"),
            )
            // Deliberately NOT showing the raw category label here (cross-framework-setup.md §D.2
            // collision note): filter option text IS the category string, so a visible row-level
            // copy of it collides with the filter panel's own option under a black-box text
            // selector. Description is enough to satisfy "title, thumbnail, description/metadata".
            Text(text = video.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}
