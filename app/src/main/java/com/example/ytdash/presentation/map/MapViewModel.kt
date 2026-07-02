package com.example.ytdash.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MapViewModel @Inject constructor(
    videoRepository: VideoRepository,
) : ViewModel() {

    private val selectedVideoId = MutableStateFlow<String?>(null)

    val state: StateFlow<MapUiState> = combine(
        videoRepository.observeVideos(),
        selectedVideoId,
    ) { videos, selectedId ->
        val located = videos.filter { it.location != null }
        MapUiState(
            locatedVideos = located,
            selectedVideo = located.find { it.id == selectedId },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MapUiState())

    fun selectVideo(videoId: String) {
        selectedVideoId.value = videoId
    }

    fun dismissDetail() {
        selectedVideoId.value = null
    }
}
