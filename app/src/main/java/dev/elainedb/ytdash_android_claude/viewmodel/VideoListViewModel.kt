package dev.elainedb.ytdash_android_claude.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.elainedb.ytdash_android_claude.model.Video
import dev.elainedb.ytdash_android_claude.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

enum class SortOption(val value: String, val displayName: String) {
    PUBLISHED_DESC("publishedAt_desc", "Publication Date (Newest First)"),
    PUBLISHED_ASC("publishedAt_asc", "Publication Date (Oldest First)"),
    RECORDING_DESC("recordingDate_desc", "Recording Date (Newest First)"),
    RECORDING_ASC("recordingDate_asc", "Recording Date (Oldest First)")
}

class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = YouTubeRepository(application)

    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.PUBLISHED_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    val availableCountries: StateFlow<List<String>> = repository.getDistinctCountries()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val availableChannels: StateFlow<List<String>> = repository.getDistinctChannels()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _totalCount = MutableStateFlow(0)

    init {
        loadVideos()
        observeVideoChanges()
        observeTotalCount()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            try {
                repository.getLatestVideos()
                // Videos will flow through observeVideoChanges
            } catch (e: Exception) {
                _uiState.value = VideoListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _sortOption) { filter, sort ->
                Pair(filter, sort)
            }.collectLatest { (filter, sort) ->
                repository.getVideosWithFiltersAndSort(
                    channelName = filter.channelName,
                    country = filter.country,
                    sortBy = sort.value
                ).collectLatest { videos ->
                    _uiState.value = if (videos.isEmpty()) {
                        VideoListUiState.Empty
                    } else {
                        VideoListUiState.Success(videos, _totalCount.value)
                    }
                }
            }
        }
    }

    private fun observeTotalCount() {
        viewModelScope.launch {
            repository.getTotalVideoCount().collectLatest { count ->
                _totalCount.value = count
                // Update success state with new total count
                val current = _uiState.value
                if (current is VideoListUiState.Success) {
                    _uiState.value = current.copy(totalCount = count)
                }
            }
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            try {
                repository.refreshVideos()
                // Videos will flow through observeVideoChanges
            } catch (e: Exception) {
                _uiState.value = VideoListUiState.Error(e.message ?: "Failed to refresh videos")
            }
        }
    }

    fun applyFilter(filter: FilterOptions) {
        _filterOptions.value = filter
    }

    fun applySorting(sort: SortOption) {
        _sortOption.value = sort
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }
}
