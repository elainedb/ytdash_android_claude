package com.example.ytdash.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.example.ytdash.domain.Video
import com.example.ytdash.ui.home.HomeScreen
import com.example.ytdash.ui.login.LoginScreen
import com.example.ytdash.ui.map.MapScreen

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RootApp(
    viewModel: MainViewModel,
    onRealSignIn: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    // Expose testTags as resource-ids to the automation layer (constitution §3).
    Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        when (state.screen) {
            Screen.LOGIN -> LoginScreen(
                authError = state.authError,
                onSignIn = { if (viewModel.config.uiTestMode) viewModel.signIn() else onRealSignIn() },
            )

            Screen.HOME -> HomeScreen(
                list = state.list,
                sort = state.sort,
                filter = state.filter,
                onRefresh = viewModel::refresh,
                onSort = viewModel::setSort,
                onFilter = viewModel::setFilter,
                onMap = viewModel::goToMap,
                onLogout = viewModel::logout,
                onOpen = { viewModel.openExternal(it.youtubeUrl) },
                onRetry = viewModel::loadVideos,
            )

            Screen.MAP -> MapScreen(
                located = (state.list as? ListUiState.Content)?.videos?.filter { it.hasLocation } ?: emptyList<Video>(),
                selected = state.selected,
                onBack = viewModel::goToHome,
                onMarker = viewModel::selectMarker,
                onDismissSheet = viewModel::dismissSheet,
                onOpen = { viewModel.openExternal(it.youtubeUrl) },
            )
        }

        // App-root external-link surfaces, shared by the list (iteration 2) and the map sheet
        // (iteration 4) so a single banner serves both.
        if (state.externalUrl != null) {
            ExternalBanner(
                modifier = Modifier.align(Alignment.TopCenter),
                tag = "external_open_url",
                title = "Opening externally",
                body = state.externalUrl!!,
                onDismiss = viewModel::dismissExternal,
            )
        }
        if (state.externalError) {
            ExternalBanner(
                modifier = Modifier.align(Alignment.TopCenter),
                tag = "external_open_error",
                title = "Couldn't open link",
                body = "No app available to open this video.",
                onDismiss = viewModel::dismissExternal,
            )
        }
    }
}

@Composable
private fun ExternalBanner(
    modifier: Modifier,
    tag: String,
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            // The tag sits on the Text whose content IS the URL, so id and text resolve to the same
            // node — the harness asserts `id:external_open_url text:<url>` on one element (§5a / §D.4).
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(tag),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Dismiss")
            }
        }
    }
}
