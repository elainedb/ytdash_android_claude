package com.example.ytdash.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.data.VideoRepository
import com.example.ytdash.data.model.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MapUiState {
    data object Loading : MapUiState
    data class Content(val located: List<Video>) : MapUiState
    data object Empty : MapUiState
    data class Error(val message: String) : MapUiState
}

class MapViewModel(private val repository: VideoRepository) : ViewModel() {

    private val _state = MutableStateFlow<MapUiState>(MapUiState.Loading)
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = MapUiState.Loading
        viewModelScope.launch {
            // Reuse the in-memory list if the home screen already loaded it; otherwise fetch.
            val inMemory = repository.cachedInMemory()
            val videos = if (inMemory != null) {
                Result.success(inMemory)
            } else {
                repository.getVideos(forceRefresh = false).map { it.videos }
            }
            videos.fold(
                onSuccess = { list ->
                    val located = list.filter { it.hasLocation }
                    _state.value = if (located.isEmpty()) MapUiState.Empty
                    else MapUiState.Content(located)
                },
                onFailure = { e ->
                    _state.value = MapUiState.Error(e.message ?: "Failed to load map data")
                },
            )
        }
    }
}
