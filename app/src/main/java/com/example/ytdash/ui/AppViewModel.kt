package com.example.ytdash.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.TestConfig
import com.example.ytdash.data.RefreshOutcome
import com.example.ytdash.data.model.Video
import com.example.ytdash.di.AppContainer
import com.example.ytdash.domain.SortMode
import com.example.ytdash.domain.VideoOps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val container: AppContainer,
    private val config: TestConfig,
) : ViewModel() {

    private val _ui = MutableStateFlow(AppUiState())
    val ui: StateFlow<AppUiState> = _ui.asStateFlow()

    /** Raw, unfiltered aggregate — the base the UI filters/sorts over. */
    private var base: List<Video> = emptyList()

    init {
        container.repository.hydrateFromCache()
        viewModelScope.launch {
            container.repository.videos.collect { latest ->
                base = latest
                recompute()
            }
        }
    }

    // ---- auth ----

    /** [realEmailProvider] is only invoked outside UI-test-mode (no mockAuthEmail). */
    fun signIn(realEmailProvider: suspend () -> String?) {
        viewModelScope.launch {
            val email = config.mockAuthEmail ?: realEmailProvider()
            if (email.isNullOrBlank()) {
                _ui.update { it.copy(loginError = "Sign-in failed. Please try again.") }
                return@launch
            }
            if (container.authService.isAuthorized(email)) {
                _ui.update { it.copy(screen = Screen.HOME, loginError = null) }
                loadVideos()
            } else {
                _ui.update { it.copy(loginError = "Access denied: $email is not authorized.") }
            }
        }
    }

    fun logout() {
        _ui.value = AppUiState(screen = Screen.LOGIN)
    }

    // ---- list ----

    fun loadVideos() {
        viewModelScope.launch {
            if (base.isEmpty()) _ui.update { it.copy(listPhase = ListPhase.LOADING) }
            val outcome = container.repository.refresh()
            val phase = when (outcome) {
                is RefreshOutcome.Success ->
                    if (outcome.videos.isEmpty()) ListPhase.EMPTY else ListPhase.CONTENT
                is RefreshOutcome.StaleFallback -> ListPhase.CONTENT
                is RefreshOutcome.Failure -> ListPhase.ERROR
            }
            _ui.update { it.copy(listPhase = phase) }
        }
    }

    fun retry() = loadVideos()

    private fun recompute() {
        val cats = VideoOps.categories(base)
        val displayed = VideoOps.apply(base, _ui.value.selectedCategory, _ui.value.sortMode)
        _ui.update {
            it.copy(
                videos = displayed,
                mapMarkers = base.filter { v -> v.hasLocation },
                totalCount = base.size,
                categories = cats,
            )
        }
    }

    // ---- filter ----

    fun openFilterPanel() = _ui.update { it.copy(filterPanelOpen = true, sortPanelOpen = false) }
    fun closeFilterPanel() = _ui.update { it.copy(filterPanelOpen = false) }

    fun selectCategory(category: String?) {
        _ui.update { it.copy(selectedCategory = category) }
        recompute()
    }

    fun applyFilter() {
        _ui.update { it.copy(filterPanelOpen = false) }
        recompute()
    }

    // ---- sort ----

    fun openSortPanel() = _ui.update { it.copy(sortPanelOpen = true, filterPanelOpen = false) }
    fun closeSortPanel() = _ui.update { it.copy(sortPanelOpen = false) }

    fun selectSort(mode: SortMode) {
        _ui.update { it.copy(sortMode = mode) }
        recompute()
    }

    fun applySort() {
        _ui.update { it.copy(sortPanelOpen = false) }
        recompute()
    }

    // ---- map ----

    fun openMap() = _ui.update { it.copy(screen = Screen.MAP, selectedVideo = null) }
    fun backToHome() = _ui.update { it.copy(screen = Screen.HOME, selectedVideo = null) }
    fun selectMarker(video: Video) = _ui.update { it.copy(selectedVideo = video) }
    fun dismissSheet() = _ui.update { it.copy(selectedVideo = null) }

    // ---- external open ----

    fun openInYouTube(url: String) {
        if (config.captureExternalLinks) {
            _ui.update { it.copy(externalUrl = url, externalError = false) }
        } else {
            val ok = container.externalLauncher.open(url)
            _ui.update { it.copy(externalError = !ok, externalUrl = null) }
        }
    }
}
