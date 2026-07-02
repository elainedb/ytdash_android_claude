package com.example.ytdash.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize().testTag("screen_login").padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "ytdash", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Sign in to see your videos", style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = { viewModel.signIn(context) },
            modifier = Modifier.padding(top = 24.dp).testTag("login_google_button"),
        ) {
            Text("Sign in with Google")
        }

        val errorText = when (val state = uiState) {
            is LoginUiState.Unauthorized -> "${state.email} is not authorized to use this app."
            is LoginUiState.SignInFailed -> state.message
            else -> null
        }
        if (errorText != null) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp).testTag("login_error_message"),
            )
        }
    }
}
