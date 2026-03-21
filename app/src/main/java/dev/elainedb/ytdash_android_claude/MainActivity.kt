package dev.elainedb.ytdash_android_claude

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.elainedb.ytdash_android_claude.ui.VideoListScreen
import dev.elainedb.ytdash_android_claude.ui.theme.YTDashAClaudeTheme
import dev.elainedb.ytdash_android_claude.viewmodel.VideoListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTDashAClaudeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: VideoListViewModel = viewModel()
                    VideoListScreen(
                        viewModel = viewModel,
                        onViewMap = {
                            startActivity(MapActivity.newIntent(this@MainActivity))
                        },
                        onLogout = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .build()
                            GoogleSignIn.getClient(this@MainActivity, gso).signOut()
                                .addOnCompleteListener {
                                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
