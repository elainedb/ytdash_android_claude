package com.example.ytdash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ytdash.TestTags
import com.example.ytdash.config.TestConfig
import com.example.ytdash.di.AppContainer
import com.example.ytdash.ui.home.HomeScreen
import com.example.ytdash.ui.home.HomeViewModel
import com.example.ytdash.ui.login.LoginScreen
import com.example.ytdash.ui.map.MapScreen
import com.example.ytdash.ui.map.MapViewModel
import com.example.ytdash.util.ExternalOpener

/**
 * Top-level composable. Sets [testTagsAsResourceId] ONCE on the root so every testTag below
 * surfaces to Maestro as a resource-id (constitution §3). All harness-asserted elements live in
 * this single composition window — no DropdownMenu/Dialog/ModalBottomSheet popups (§5a).
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun AppRoot(container: AppContainer) {
    val config = container.config
    val authVm: AuthViewModel = viewModel(
        factory = viewModelFactory { initializer { AuthViewModel(config.authorizedEmails) } },
    )
    val authState by authVm.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        when (authState) {
            is AuthState.SignedIn -> SignedInApp(container, authVm)
            is AuthState.SignedOut -> LoginScreen(
                config = config,
                error = (authState as AuthState.SignedOut).error,
                onEmailSignedIn = authVm::onEmailSignedIn,
                onSignInError = authVm::onSignInError,
            )
        }
    }
}

private enum class Route { HOME, MAP }

@Composable
private fun SignedInApp(container: AppContainer, authVm: AuthViewModel) {
    val config = container.config
    val context = LocalContext.current

    val homeVm: HomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HomeViewModel(container.repository) } },
    )
    val mapVm: MapViewModel = viewModel(
        factory = viewModelFactory { initializer { MapViewModel(container.repository) } },
    )

    var route by rememberSaveable { mutableStateOf(Route.HOME) }

    // External-open state, lifted to the app root so the SAME banner serves both the list
    // (iteration 2) and the map bottom sheet (iteration 4).
    var capturedUrl by remember { mutableStateOf<String?>(null) }
    var externalError by remember { mutableStateOf(false) }

    val openExternal: (String) -> Unit = remember(config) {
        { url ->
            if (config.captureExternalLinks) {
                capturedUrl = url
                externalError = false
            } else {
                val ok = ExternalOpener.launch(context, url)
                externalError = !ok
                if (ok) capturedUrl = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (route) {
            Route.HOME -> HomeScreen(
                viewModel = homeVm,
                onOpenVideo = openExternal,
                onNavigateToMap = { route = Route.MAP },
                onLogout = authVm::signOut,
            )

            Route.MAP -> MapScreen(
                viewModel = mapVm,
                onOpenVideo = openExternal,
                onBack = { route = Route.HOME },
            )
        }

        // ---- External-open banner overlay (visible across all screens) ----
        capturedUrl?.let { url ->
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = url,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTagAs(TestTags.EXTERNAL_OPEN_URL),
                )
            }
        }
        if (externalError) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = "Couldn't open the video externally.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTagAs(TestTags.EXTERNAL_OPEN_ERROR),
                )
            }
        }
    }
}
