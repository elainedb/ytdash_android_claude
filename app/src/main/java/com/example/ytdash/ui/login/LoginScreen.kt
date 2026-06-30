package com.example.ytdash.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ytdash.ui.common.tag

@Composable
fun LoginScreen(
    authError: String?,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .tag("screen_login")
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("YouTube Dashboard", style = MaterialTheme.typography.headlineSmall)
        Button(
            onClick = onSignIn,
            modifier = Modifier
                .padding(top = 24.dp)
                .tag("login_google_button"),
        ) {
            Text("Sign in with Google")
        }
        if (authError != null) {
            Text(
                text = authError,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(320.dp)
                    .tag("login_error_message"),
            )
        }
    }
}
