package com.example.ytdash.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.domain.model.Video
import com.example.ytdash.domain.repo.AuthRepository
import com.example.ytdash.domain.repo.VideoRepository
import com.example.ytdash.domain.usecase.SortOrder
import com.example.ytdash.domain.usecase.VideoFilter
import com.example.ytdash.domain.usecase.VideoSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(
        val videos: List<Video>,
        val totalCount: Int,
        val categories: List<String>,
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // null = natural fetch order (unsorted) until the user explicitly applies a sort. AC-LIST-03
    // taps index 0 expecting the first-fetched video (VIDEO_ID_1); defaulting to a date sort here
    // would silently reorder the list before the user ever touched sort_button.
    private val sortOrder = MutableStateFlow<SortOrder?>(null)
    private val filterCategory = MutableStateFlow<String?>(null)
    private val loadError = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        videoRepository.observeVideos(),
        sortOrder,
        filterCategory,
        loadError,
        isRefreshing,
    ) { videos, sort, filter, error, refreshing ->
        when {
            // Stale-cache fallback (constitution §1.5): a failed refresh only blocks the screen
            // if there is truly nothing cached to fall back to.
            videos.isEmpty() && error != null && !refreshing -> HomeUiState.Error(error)
            videos.isEmpty() && refreshing -> HomeUiState.Loading
            else -> {
                val filtered = VideoFilter.apply(videos, filter)
                val display = if (sort != null) VideoSort.apply(filtered, sort) else filtered
                val categories = videos.map { it.category }.distinct().sorted()
                HomeUiState.Content(videos = display, totalCount = videos.size, categories = categories)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            videoRepository.refresh().fold(
                onSuccess = { loadError.value = null },
                onFailure = { loadError.value = it.message ?: "Couldn't load videos." },
            )
            isRefreshing.value = false
        }
    }

    fun retry() = refresh()

    fun setSort(order: SortOrder) {
        sortOrder.value = order
    }

    fun setFilter(category: String?) {
        filterCategory.value = category
    }

    fun logout() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
