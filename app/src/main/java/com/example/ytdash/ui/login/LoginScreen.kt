package com.example.ytdash.ui.login

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ytdash.TestTags
import com.example.ytdash.config.TestConfig
import com.example.ytdash.ui.testTagAs
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    config: TestConfig,
    error: String?,
    onEmailSignedIn: (String?) -> Unit,
    onSignInError: (String) -> Unit,
) {
    val context = LocalContext.current

    // Real Google sign-in path (production / non-test mode). Works without google-services.json:
    // we only request the verified email, then run the same whitelist logic the mock path uses.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            onEmailSignedIn(account?.email)
        } catch (e: ApiException) {
            onSignInError("Google sign-in failed (code ${e.statusCode}).")
        } catch (e: Exception) {
            onSignInError("Google sign-in failed.")
        }
    }

    fun signIn() {
        // UI-test-mode: skip the real account picker and sign in as the supplied mock email,
        // then normal whitelist logic runs (constitution §4).
        val mockEmail = config.mockAuthEmail
        if (config.uiTestMode && mockEmail != null) {
            onEmailSignedIn(mockEmail)
            return
        }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        // Sign out first so the picker always shows (avoids silently reusing a stale account).
        client.signOut().addOnCompleteListener {
            launcher.launch(client.signInIntent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTagAs(TestTags.SCREEN_LOGIN),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "YouTube Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Sign in with an authorized Google account to continue.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Button(
            onClick = { signIn() },
            modifier = Modifier.testTagAs(TestTags.LOGIN_GOOGLE_BUTTON),
        ) {
            Text("Sign in with Google")
        }
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .testTagAs(TestTags.LOGIN_ERROR_MESSAGE),
            )
        }
    }
}
