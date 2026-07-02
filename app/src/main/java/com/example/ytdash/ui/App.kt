package com.example.ytdash.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Root composable. A single [Box] carries `testTagsAsResourceId = true` so every `testTag` in the
 * (single-composition) tree surfaces to Maestro as a resource-id. All harness-asserted elements —
 * including the filter/sort panels, the map detail sheet, and the external-open banner — live in
 * THIS composition (no DropdownMenu/Dialog/ModalBottomSheet), so §5a's popup trap is avoided.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(
    viewModel: AppViewModel,
    onRealSignIn: suspend () -> String?,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Keep content out from under the status/navigation bars so top-of-screen elements
            // (video_count, logout_button) are visible & tappable to the automation layer.
            .safeDrawingPadding()
            .semantics { testTagsAsResourceId = true },
    ) {
        when (ui.screen) {
            Screen.LOGIN -> LoginScreen(ui = ui, onSignIn = { viewModel.signIn(onRealSignIn) })
            Screen.HOME -> HomeScreen(ui = ui, viewModel = viewModel)
            Screen.MAP -> MapScreen(ui = ui, viewModel = viewModel)
        }

        // App-root external-open feedback, shared by the list (iteration 2) and the map (iteration 4).
        if (ui.externalUrl != null) {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = ui.externalUrl!!,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag(Tags.EXTERNAL_OPEN_URL),
                )
            }
        }
        if (ui.externalError) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Couldn't open the external app.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag(Tags.EXTERNAL_OPEN_ERROR),
                )
            }
        }
    }
}
