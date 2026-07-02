package com.example.ytdash.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.BuildConfig
import com.example.ytdash.domain.repo.AuthRepository
import com.example.ytdash.domain.usecase.AuthPolicy
import com.example.ytdash.testmode.TestConfigProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Unauthorized(val email: String) : LoginUiState
    data class SignInFailed(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val testConfigProvider: TestConfigProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(context: Context) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val cfg = testConfigProvider.current
            val whitelist = cfg.authorizedEmails?.takeIf { it.isNotBlank() }
                ?: BuildConfig.DEFAULT_AUTHORIZED_EMAILS

            val emailResult: Result<String> =
                if (cfg.uiTestMode && !cfg.mockAuthEmail.isNullOrBlank()) {
                    Result.success(cfg.mockAuthEmail)
                } else {
                    authRepository.signInWithGoogle(context)
                }

            emailResult.fold(
                onSuccess = { email ->
                    if (AuthPolicy.isAuthorized(email, whitelist)) {
                        authRepository.persistSession(email)
                        _uiState.value = LoginUiState.Idle
                    } else {
                        _uiState.value = LoginUiState.Unauthorized(email)
                    }
                },
                onFailure = { e ->
                    _uiState.value = LoginUiState.SignInFailed(e.message ?: "Sign-in failed")
                },
            )
        }
    }
}
