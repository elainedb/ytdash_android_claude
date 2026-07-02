package com.example.ytdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.example.ytdash.core.AppConfig
import com.example.ytdash.core.ExternalLinkLauncher
import com.example.ytdash.core.TestConfig
import com.example.ytdash.theme.YtdashTheme
import com.example.ytdash.ui.common.ExternalLinkBanner
import com.example.ytdash.ui.home.HomeScreen
import com.example.ytdash.ui.login.LoginScreen
import com.example.ytdash.ui.map.MapScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private sealed interface Screen {
    data object Login : Screen
    data object Home : Screen
    data object Map : Screen
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appConfig: AppConfig

    @Inject lateinit var externalLinkLauncher: ExternalLinkLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Must happen before any ViewModel/repository is created (constitution §4) — Hilt
        // providers that read AppConfig (base URL, whitelist) resolve lazily on first
        // injection, which only happens once Compose content below actually renders a screen.
        appConfig.applyTestConfig(TestConfig.fromIntent(intent))

        setContent {
            YtdashTheme {
                AppRoot(externalLinkLauncher = externalLinkLauncher)
            }
        }
    }
}

@Composable
private fun AppRoot(externalLinkLauncher: ExternalLinkLauncher) {
    var screen by remember { mutableStateOf<Screen>(Screen.Login) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
    ) {
        when (screen) {
            Screen.Login -> LoginScreen(onLoggedIn = { screen = Screen.Home })
            Screen.Home -> HomeScreen(
                onLogout = { screen = Screen.Login },
                onOpenMap = { screen = Screen.Map },
            )
            Screen.Map -> MapScreen(onBack = { screen = Screen.Home })
        }

        ExternalLinkBanner(
            launcher = externalLinkLauncher,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
