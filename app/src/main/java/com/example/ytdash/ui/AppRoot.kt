package com.example.ytdash.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ytdash.ui.common.ExternalLinkBanner
import com.example.ytdash.ui.common.ExternalLinkLauncher
import com.example.ytdash.ui.common.ExternalLinkState
import com.example.ytdash.ui.home.HomeScreen
import com.example.ytdash.ui.login.LoginScreen
import com.example.ytdash.ui.map.MapScreen

private sealed interface Screen {
    data object Home : Screen
    data object Map : Screen
}

/**
 * The whole app has 3 linear screens (login → home → map, one back edge), so this is a
 * hand-rolled sealed-state switch rather than a navigation library (plan.md). Also hosts the two
 * pieces of state the constitution says must be app-root-level: `testTagsAsResourceId` (§3, set
 * once at the top of the tree) and the external-link capture/error banner (cross-framework-setup
 * §D — shared by both the list and the map detail sheet).
 */
@Composable
fun AppRoot(rootViewModel: AppRootViewModel = hiltViewModel()) {
    val sessionEmail by rootViewModel.sessionEmail.collectAsStateWithLifecycle()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var externalLinkState by remember { mutableStateOf<ExternalLinkState>(ExternalLinkState.Idle) }
    val context = LocalContext.current

    LaunchedEffect(sessionEmail) {
        if (sessionEmail == null) screen = Screen.Home
    }

    BackHandler(enabled = screen is Screen.Map) { screen = Screen.Home }

    val onOpenExternalLink: (String) -> Unit = { url ->
        val capture = rootViewModel.testConfigProvider.current.captureExternalLinks
        externalLinkState = ExternalLinkLauncher.launch(context, url, capture)
    }

    Box(
        modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
    ) {
        if (sessionEmail == null) {
            LoginScreen()
        } else {
            when (screen) {
                Screen.Home -> HomeScreen(
                    onNavigateMap = { screen = Screen.Map },
                    onOpenExternalLink = onOpenExternalLink,
                )
                Screen.Map -> MapScreen(onOpenExternalLink = onOpenExternalLink)
            }
        }
        // statusBarsPadding: a banner drawn at y=0 sits fully behind the status bar's own system
        // window, which Android's accessibility layer treats as "obscured" (not visible to the
        // user) — the same class of bug as the top-bar buttons above (see HomeScreen.kt).
        ExternalLinkBanner(
            state = externalLinkState,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        )
    }
}
