package com.example.ytdash.ui

import androidx.lifecycle.ViewModel
import com.example.ytdash.domain.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthState {
    data class SignedOut(val error: String? = null) : AuthState
    data class SignedIn(val email: String) : AuthState
}

/** Holds auth/access view-state. Whitelist evaluation is delegated to the domain layer. */
class AuthViewModel(private val whitelist: List<String>) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Called once an email has been obtained (mock or real Google sign-in). */
    fun onEmailSignedIn(email: String?) {
        _state.value = if (Auth.isAuthorized(email, whitelist)) {
            AuthState.SignedIn(email!!.trim())
        } else {
            AuthState.SignedOut("Access denied: this account is not authorized.")
        }
    }

    /** Real Google sign-in failed (cancelled, no network, no Play services, etc.). */
    fun onSignInError(message: String) {
        _state.value = AuthState.SignedOut(message)
    }

    fun signOut() {
        _state.value = AuthState.SignedOut()
    }
}
