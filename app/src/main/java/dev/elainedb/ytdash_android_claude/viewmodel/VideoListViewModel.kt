package dev.elainedb.ytdash_android_claude.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.elainedb.ytdash_android_claude.model.Video
import dev.elainedb.ytdash_android_claude.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

enum class SortOption(val value: String) {
    PUBLISHED_DESC("published_desc"),
    PUBLISHED_ASC("published_asc"),
    RECORDING_DESC("recording_desc"),
    RECORDING_ASC("recording_asc")
}

class VideoListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = YouTubeRepository(application)

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
            try {
                repository.getLatestVideos()
            } catch (e: Exception) {
                _uiState.value = VideoListUiState.Error(e.message ?: "Failed to load videos")
            }
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            try {
                repository.refreshVideos()
            } catch (e: Exception) {
                _uiState.value = VideoListUiState.Error(e.message ?: "Failed to refresh videos")
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
                    val totalCount = videos.size
                    _uiState.value = if (videos.isEmpty()) {
                        VideoListUiState.Empty
                    } else {
                        VideoListUiState.Success(videos, totalCount)
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.getTotalVideoCount().collectLatest { count ->
                val currentState = _uiState.value
                if (currentState is VideoListUiState.Success) {
                    _uiState.value = currentState.copy(totalCount = count)
                }
            }
        }
    }

    private fun observeFilterOptions() {
        viewModelScope.launch {
            repository.getDistinctCountries().collectLatest {
                _availableCountries.value = it
            }
        }
        viewModelScope.launch {
            repository.getDistinctChannels().collectLatest {
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
}
