package com.example.ytdash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ytdash.di.AppContainer
import com.example.ytdash.theme.YtdashTheme
import com.example.ytdash.ui.App
import com.example.ytdash.ui.AppViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CompletableDeferred

class MainActivity : ComponentActivity() {

    private lateinit var config: TestConfig
    private lateinit var container: AppContainer

    private var googleClient: GoogleSignInClient? = null
    private var pendingSignIn: CompletableDeferred<String?>? = null

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val email = try {
                GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)?.email
            } catch (_: Exception) {
                null
            }
            pendingSignIn?.complete(email)
            pendingSignIn = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = TestConfig.fromIntent(intent)
        container = AppContainer(applicationContext, config)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppViewModel(container, config) as T
        }

        enableEdgeToEdge()
        setContent {
            YtdashTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: AppViewModel = viewModel(factory = factory)
                    App(viewModel = vm, onRealSignIn = ::realSignIn)
                }
            }
        }
    }

    /** Real Google Sign-In (only used outside UI-test-mode). Returns the verified email or null. */
    private suspend fun realSignIn(): String? {
        GoogleSignIn.getLastSignedInAccount(this)?.email?.let { return it }
        val client = googleClient ?: GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build(),
        ).also { googleClient = it }
        val deferred = CompletableDeferred<String?>()
        pendingSignIn = deferred
        signInLauncher.launch(client.signInIntent)
        return deferred.await()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
