package com.example.ytdash.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ytdash.config.TestConfig
import com.example.ytdash.domain.AuthGate
import com.example.ytdash.domain.LoadResult
import com.example.ytdash.domain.SortOption
import com.example.ytdash.domain.Video
import com.example.ytdash.domain.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { LOGIN, HOME, MAP }

sealed interface ListUiState {
    data object Loading : ListUiState
    data class Content(val videos: List<Video>, val fromCache: Boolean) : ListUiState
    data object Empty : ListUiState
    data class Error(val message: String) : ListUiState
}

/** Single observable view-state for the whole app (constitution §1.3). */
data class AppUiState(
    val screen: Screen = Screen.LOGIN,
    val signedInEmail: String? = null,
    val authError: String? = null,
    val list: ListUiState = ListUiState.Loading,
    val sort: SortOption = SortOption.DEFAULT,
    val filter: String? = null,
    val selected: Video? = null,
    val externalUrl: String? = null,
    val externalError: Boolean = false,
)

class MainViewModel(
    application: Application,
    val config: TestConfig,
    private val repository: VideoRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    /** Sign-in entry point used by the login button. In UI-test-mode the email is injected. */
    fun signIn(realEmail: String? = null) {
        val email = if (config.uiTestMode) config.mockAuthEmail ?: realEmail else realEmail
        completeSignIn(email)
    }

    fun completeSignIn(email: String?) {
        if (AuthGate.isAuthorized(email, config.authorizedEmails)) {
            _state.update { it.copy(screen = Screen.HOME, signedInEmail = email, authError = null) }
            loadVideos()
        } else {
            _state.update {
                it.copy(
                    screen = Screen.LOGIN,
                    authError = "Access denied: ${email ?: "this account"} is not authorized.",
                )
            }
        }
    }

    fun onSignInFailed(message: String) {
        _state.update { it.copy(authError = message) }
    }

    fun logout() {
        _state.value = AppUiState() // back to a clean LOGIN state
    }

    fun loadVideos() {
        _state.update { it.copy(list = ListUiState.Loading) }
        viewModelScope.launch {
            repository.loadVideos().collect { result ->
                val next = when (result) {
                    is LoadResult.Success ->
                        if (result.videos.isEmpty()) ListUiState.Empty
                        else ListUiState.Content(result.videos, result.fromCache)
                    is LoadResult.Failure -> ListUiState.Error(result.message)
                }
                _state.update { it.copy(list = next) }
            }
        }
    }

    fun refresh() = loadVideos()

    fun setSort(option: SortOption) = _state.update { it.copy(sort = option) }

    fun setFilter(label: String?) = _state.update { it.copy(filter = label) }

    fun goToMap() = _state.update { it.copy(screen = Screen.MAP, selected = null) }

    fun goToHome() = _state.update { it.copy(screen = Screen.HOME, selected = null) }

    fun selectMarker(video: Video) = _state.update { it.copy(selected = video) }

    fun dismissSheet() = _state.update { it.copy(selected = null) }

    /**
     * Open a video externally. In capture mode (UI-test) we surface the target URL instead of
     * launching; otherwise we perform the real launch and surface an error if it fails — never crash.
     */
    fun openExternal(url: String) {
        if (config.captureExternalLinks) {
            _state.update { it.copy(externalUrl = url, externalError = false) }
        } else {
            val ok = runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            }.isSuccess
            _state.update { it.copy(externalError = !ok, externalUrl = null) }
        }
    }

    fun dismissExternal() = _state.update { it.copy(externalUrl = null, externalError = false) }

    class Factory(
        private val application: Application,
        private val config: TestConfig,
        private val repository: VideoRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(application, config, repository) as T
    }
}
