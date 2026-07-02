package com.example.ytdash.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedIn by viewModel.loggedIn.collectAsStateWithLifecycle()

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> viewModel.onGoogleSignInResult(result.data) }

    LaunchedEffect(loggedIn) {
        if (loggedIn) {
            onLoggedIn()
            viewModel.consumeLoggedIn()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().testTag("screen_login").padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("ytdash", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        Button(
            modifier = Modifier.testTag("login_google_button"),
            onClick = { viewModel.onSignInClicked { intent -> googleLauncher.launch(intent) } },
        ) {
            Text("Sign in with Google")
        }
        val state = uiState
        if (state is LoginUiState.Error) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.message,
                modifier = Modifier.testTag("login_error_message"),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
