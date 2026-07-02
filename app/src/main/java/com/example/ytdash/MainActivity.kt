package com.example.ytdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ytdash.testmode.TestConfig
import com.example.ytdash.testmode.TestConfigProvider
import com.example.ytdash.theme.YtdashTheme
import com.example.ytdash.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var testConfigProvider: TestConfigProvider

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Constitution §4: parse the UI-test-mode launch extras BEFORE any UI is shown, so every
    // downstream read (network base URL/key, auth whitelist, external-link capture) sees them.
    testConfigProvider.update(TestConfig.fromIntent(intent))

    enableEdgeToEdge()
    setContent {
      YtdashTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AppRoot()
        }
      }
    }
  }
}
