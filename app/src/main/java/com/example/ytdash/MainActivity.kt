package com.example.ytdash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ytdash.core.config.RuntimeConfig
import com.example.ytdash.core.link.ExternalLinkViewModel
import com.example.ytdash.core.testmode.TestConfig
import com.example.ytdash.presentation.common.ExternalLinkBanner
import com.example.ytdash.presentation.navigation.YtDashNavHost
import com.example.ytdash.presentation.theme.YtDashTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var runtimeConfig: RuntimeConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyTestConfigFromIntent(intent)

        setContent {
            YtDashTheme {
                // Set once on the root, this is what surfaces every descendant `testTag` as a
                // stable resource-id to the accessibility/automation layer (constitution §3).
                Box(modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                    val externalLinkViewModel: ExternalLinkViewModel = hiltViewModel()
                    YtDashNavHost(externalLinkViewModel = externalLinkViewModel, modifier = Modifier.fillMaxSize())
                    val event by externalLinkViewModel.event.collectAsState()
                    // statusBarsPadding: this banner lives outside Scaffold's own inset handling
                    // (it must overlay both the list and map screens), so it needs its own
                    // status-bar inset — otherwise edge-to-edge draws it under the status bar,
                    // where it's obscured from the accessibility/automation layer.
                    ExternalLinkBanner(
                        event = event,
                        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyTestConfigFromIntent(intent)
    }

    private fun applyTestConfigFromIntent(intent: Intent?) {
        runtimeConfig.applyTestConfig(TestConfig.fromIntent(intent))
    }
}
