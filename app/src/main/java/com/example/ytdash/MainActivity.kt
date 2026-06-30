package com.example.ytdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.ytdash.config.TestConfig
import com.example.ytdash.ui.MainViewModel
import com.example.ytdash.ui.RootApp
import com.example.ytdash.ui.theme.YtdashTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var googleClient: GoogleSignInClient

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                viewModel.completeSignIn(account.email)
            } catch (e: Exception) {
                viewModel.onSignInFailed("Google sign-in failed. Please try again.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = TestConfig.fromIntent(intent)
        val container = (application as YtdashApp).container
        val repository = container.repository(config)

        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(application, config, repository),
        )[MainViewModel::class.java]

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        setContent {
            YtdashTheme {
                RootApp(viewModel = viewModel, onRealSignIn = ::startRealSignIn)
            }
        }
    }

    private fun startRealSignIn() {
        signInLauncher.launch(googleClient.signInIntent)
    }
}
