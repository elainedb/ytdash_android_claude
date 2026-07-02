package com.example.ytdash.presentation.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.core.config.RuntimeConfig
import com.example.ytdash.domain.repository.AuthRepository
import com.example.ytdash.domain.usecase.IsAuthorizedEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val runtimeConfig: RuntimeConfig,
    private val isAuthorizedEmailUseCase: IsAuthorizedEmailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onSignInClicked(context: Context) {
        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            val config = runtimeConfig.state.value

            val emailResult: Result<String> = if (config.uiTestMode) {
                val mockEmail = config.mockAuthEmail
                if (mockEmail.isNullOrBlank()) {
                    Result.failure(IllegalStateException("uiTestMode is on but no mockAuthEmail was provided."))
                } else {
                    Result.success(mockEmail)
                }
            } else {
                authRepository.signInWithGoogle(context)
            }

            emailResult.fold(
                onSuccess = { email ->
                    if (isAuthorizedEmailUseCase(config.authorizedEmails, email)) {
                        authRepository.signInAs(email)
                        _state.value = LoginUiState.Success
                    } else {
                        _state.value = LoginUiState.Error("This account isn't authorized to use ytdash.")
                    }
                },
                onFailure = { error ->
                    _state.value = LoginUiState.Error(error.message ?: "Sign-in failed. Please try again.")
                },
            )
        }
    }

    fun consumeSuccess() {
        _state.value = LoginUiState.Idle
    }
}
