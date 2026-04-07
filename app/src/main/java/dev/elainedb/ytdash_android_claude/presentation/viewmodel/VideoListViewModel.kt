package dev.elainedb.ytdash_android_claude.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.domain.model.Video
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import dev.elainedb.ytdash_android_claude.domain.usecases.GetVideos
import dev.elainedb.ytdash_android_claude.domain.usecases.GetVideosParams
import dev.elainedb.ytdash_android_claude.domain.usecases.SignOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VideoListUiState {
    data object Loading : VideoListUiState()
    data object Empty : VideoListUiState()
    data class Success(val videos: List<Video>, val totalCount: Int) : VideoListUiState()
    data class Error(val message: String) : VideoListUiState()
}

data class FilterOptions(
    val channelName: String? = null,
    val country: String? = null
)

enum class SortOption(val value: String, val label: String) {
    PUBLISHED_DESC("publishedAt_desc", "Publication Date (Newest First)"),
    PUBLISHED_ASC("publishedAt_asc", "Publication Date (Oldest First)"),
    RECORDING_DESC("recordingDate_desc", "Recording Date (Newest First)"),
    RECORDING_ASC("recordingDate_asc", "Recording Date (Oldest First)")
}

@HiltViewModel
class VideoListViewModel @Inject constructor(
    private val getVideos: GetVideos,
    private val signOut: SignOut,
    private val videoRepository: VideoRepository
) : ViewModel() {

    companion object {
        val CHANNEL_IDS = listOf(
            "UCynoa1DjwnvHAowA_jiMEAQ",
            "UCK0KOjX3beyB9nzonls0cuw",
            "UCACkIrvrGAQ7kuc0hMVwvmA",
            "UCtWRAKKvOEA0CXOue9BG8ZA"
        )
    }

    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.PUBLISHED_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _availableCountries = MutableStateFlow<List<String>>(emptyList())
    val availableCountries: StateFlow<List<String>> = _availableCountries.asStateFlow()

    private val _availableChannels = MutableStateFlow<List<String>>(emptyList())
    val availableChannels: StateFlow<List<String>> = _availableChannels.asStateFlow()

    init {
        loadVideos()
        observeVideoChanges()
        observeFilterOptions()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            when (val result = getVideos(GetVideosParams(CHANNEL_IDS))) {
                is Result.Success -> {
                    // Videos are now in DB, the Flow observer will update UI
                }
                is Result.Error -> {
                    _uiState.value = VideoListUiState.Error(result.failure.message)
                }
            }
        }
    }

    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _sortOption) { filter, sort ->
                Pair(filter, sort)
            }.collectLatest { (filter, sort) ->
                videoRepository.getVideosWithFiltersAndSort(
                    filter.channelName, filter.country, sort.value
                ).collectLatest { videos ->
                    val totalCount = videoRepository.getTotalVideoCount()
                    _uiState.value = if (videos.isEmpty()) {
                        VideoListUiState.Empty
                    } else {
                        VideoListUiState.Success(videos, totalCount)
                    }
                }
            }
        }
    }

    private fun observeFilterOptions() {
        viewModelScope.launch {
            videoRepository.getDistinctCountries().collectLatest {
                _availableCountries.value = it
            }
        }
        viewModelScope.launch {
            videoRepository.getDistinctChannels().collectLatest {
                _availableChannels.value = it
            }
        }
    }

    fun applyFilter(filterOptions: FilterOptions) {
        _filterOptions.value = filterOptions
    }

    fun applySorting(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            when (val result = getVideos(GetVideosParams(CHANNEL_IDS, forceRefresh = true))) {
                is Result.Success -> {
                    // Flow observer will update UI
                }
                is Result.Error -> {
                    _uiState.value = VideoListUiState.Error(result.failure.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            signOut(Unit)
        }
    }
}
