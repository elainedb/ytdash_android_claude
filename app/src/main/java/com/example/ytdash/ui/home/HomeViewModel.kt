package com.example.ytdash.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.core.ExternalLinkLauncher
import com.example.ytdash.data.auth.AuthRepository
import com.example.ytdash.data.local.VideoCacheRepository
import com.example.ytdash.domain.FilterSpec
import com.example.ytdash.domain.SortSpec
import com.example.ytdash.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(
        val videos: List<Video>,
        val totalCount: Int,
        val categories: List<String>,
        val filter: FilterSpec,
        val sort: SortSpec,
        val isRefreshing: Boolean,
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cacheRepository: VideoCacheRepository,
    private val authRepository: AuthRepository,
    private val externalLinkLauncher: ExternalLinkLauncher,
) : ViewModel() {

    private val filterState = MutableStateFlow(FilterSpec())
    private val sortState = MutableStateFlow(SortSpec.Natural)
    private val isRefreshing = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        cacheRepository.observeVideos(),
        filterState,
        sortState,
        isRefreshing,
    ) { videos, filter, sort, refreshing ->
        val error = errorMessage.value
        when {
            videos.isEmpty() && refreshing -> HomeUiState.Loading
            videos.isEmpty() && error != null -> HomeUiState.Error(error)
            else -> HomeUiState.Content(
                videos = sort.apply(filter.apply(videos)),
                totalCount = videos.size,
                categories = videos.map { it.category }.distinct().sorted(),
                filter = filter,
                sort = sort,
                isRefreshing = refreshing,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            cacheRepository.refresh()
                .onSuccess { errorMessage.value = null }
                .onFailure { errorMessage.value = it.message ?: "Failed to load videos" }
            isRefreshing.value = false
        }
    }

    fun applyFilter(category: String?) {
        filterState.value = FilterSpec(category)
    }

    fun applySort(sort: SortSpec) {
        sortState.value = sort
    }

    fun openVideo(video: Video) {
        externalLinkLauncher.open(video.youtubeUrl)
    }

    fun logout() {
        authRepository.signOut()
    }
}
