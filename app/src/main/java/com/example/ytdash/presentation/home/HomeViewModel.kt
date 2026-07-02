package com.example.ytdash.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.domain.model.SortOption
import com.example.ytdash.domain.repository.AuthRepository
import com.example.ytdash.domain.repository.VideoRepository
import com.example.ytdash.domain.usecase.FilterVideosUseCase
import com.example.ytdash.domain.usecase.SortVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class UiFlags(
    // null = natural (as-loaded) order — AC-LIST-03/AC-MAP-03 depend on the first row being the
    // fixture's natural order until the user explicitly chooses a sort.
    val sortOption: SortOption? = null,
    val filterCategory: String? = null,
    val panelMode: PanelMode = PanelMode.NONE,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val authRepository: AuthRepository,
    private val sortVideosUseCase: SortVideosUseCase,
    private val filterVideosUseCase: FilterVideosUseCase,
) : ViewModel() {

    private val uiFlags = MutableStateFlow(UiFlags())

    val state: StateFlow<HomeUiState> = combine(videoRepository.observeVideos(), uiFlags) { videos, flags ->
        when {
            videos.isEmpty() && flags.isLoading -> HomeUiState.Loading
            videos.isEmpty() && flags.error != null -> HomeUiState.Error(flags.error)
            else -> {
                val filtered = filterVideosUseCase(videos, flags.filterCategory)
                val sorted = flags.sortOption?.let { sortVideosUseCase(filtered, it) } ?: filtered
                HomeUiState.Content(
                    displayVideos = sorted,
                    totalCount = videos.size,
                    sortOption = flags.sortOption,
                    filterCategory = flags.filterCategory,
                    availableCategories = videos.map { it.category }.distinct().sorted(),
                    panelMode = flags.panelMode,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            uiFlags.update { it.copy(isLoading = true) }
            videoRepository.refresh()
                .onSuccess { uiFlags.update { it.copy(error = null) } }
                .onFailure { e -> uiFlags.update { it.copy(error = e.message ?: "Failed to load videos.") } }
            uiFlags.update { it.copy(isLoading = false) }
        }
    }

    fun openFilterPanel() = uiFlags.update { it.copy(panelMode = PanelMode.FILTER) }
    fun openSortPanel() = uiFlags.update { it.copy(panelMode = PanelMode.SORT) }
    fun closePanel() = uiFlags.update { it.copy(panelMode = PanelMode.NONE) }
    fun selectFilter(category: String?) = uiFlags.update { it.copy(filterCategory = category, panelMode = PanelMode.NONE) }
    fun selectSort(option: SortOption) = uiFlags.update { it.copy(sortOption = option, panelMode = PanelMode.NONE) }
    fun logout() = authRepository.signOut()
}
