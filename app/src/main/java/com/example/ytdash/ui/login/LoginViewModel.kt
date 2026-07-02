package com.example.ytdash.ui.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.example.ytdash.data.auth.AuthOutcome
import com.example.ytdash.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    fun onSignInClicked(onLaunchGoogleIntent: (Intent) -> Unit) {
        _uiState.value = LoginUiState.Idle
        if (authRepository.isMockMode) {
            handleOutcome(authRepository.signInWithMock())
        } else {
            onLaunchGoogleIntent(authRepository.googleSignInIntent())
        }
    }

    fun onGoogleSignInResult(data: Intent?) {
        handleOutcome(authRepository.handleGoogleSignInResult(data))
    }

    /**
     * One-shot consume: this ViewModel is scoped to the Activity (there's no back-stack entry
     * to dispose it), so without resetting, re-entering the login screen after a logout would
     * see a stale `loggedIn=true` and immediately navigate back to home.
     */
    fun consumeLoggedIn() {
        _loggedIn.value = false
    }

    private fun handleOutcome(outcome: AuthOutcome) {
        when (outcome) {
            is AuthOutcome.Authorized -> _loggedIn.value = true
            is AuthOutcome.Unauthorized ->
                _uiState.value = LoginUiState.Error("This account is not authorized to use ytdash.")
            is AuthOutcome.Failure -> _uiState.value = LoginUiState.Error(outcome.message)
        }
    }
}
