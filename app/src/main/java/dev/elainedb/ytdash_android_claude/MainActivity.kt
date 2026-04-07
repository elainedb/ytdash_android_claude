package dev.elainedb.ytdash_android_claude

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import dev.elainedb.ytdash_android_claude.presentation.login.LoginActivity
import dev.elainedb.ytdash_android_claude.presentation.map.MapActivity
import dev.elainedb.ytdash_android_claude.presentation.videolist.VideoListScreen
import dev.elainedb.ytdash_android_claude.presentation.videolist.VideoListViewModel
import dev.elainedb.ytdash_android_claude.ui.theme.YTDashAClaudeTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: VideoListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTDashAClaudeTheme {
                VideoListScreen(
                    viewModel = viewModel,
                    onLogout = {
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    },
                    onViewMap = {
                        startActivity(MapActivity.newIntent(this))
                    }
                )
            }
        }
    }
}
