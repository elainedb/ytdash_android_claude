package com.example.ytdash.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.domain.model.Video
import com.example.ytdash.domain.repo.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    videoRepository: VideoRepository,
) : ViewModel() {

    val locatedVideos: StateFlow<List<Video>> = videoRepository.observeVideos()
        .map { videos -> videos.filter { it.hasLocation } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedVideoId = MutableStateFlow<String?>(null)
    val selectedVideoId: StateFlow<String?> = _selectedVideoId.asStateFlow()

    fun select(videoId: String) {
        _selectedVideoId.value = videoId
    }

    fun clearSelection() {
        _selectedVideoId.value = null
    }
}
