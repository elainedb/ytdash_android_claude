package com.example.ytdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ytdash.config.TestConfig
import com.example.ytdash.di.AppContainer
import com.example.ytdash.theme.YTDashTheme
import com.example.ytdash.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read the UI-test-mode launch extras ONCE from the launching intent (constitution §4),
        // then build the DI container with the resolved apiBaseUrl/apiKey/whitelist for this run.
        val config = TestConfig.fromIntent(intent)
        val container = AppContainer(applicationContext, config)

        enableEdgeToEdge()
        setContent {
            YTDashTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot(container)
                }
            }
        }
    }
}
