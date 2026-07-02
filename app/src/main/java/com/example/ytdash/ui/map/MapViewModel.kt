package com.example.ytdash.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.core.ExternalLinkLauncher
import com.example.ytdash.data.local.VideoCacheRepository
import com.example.ytdash.domain.model.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Reads located videos from the Room-backed cache — no separate network call (constitution §1.5). */
@HiltViewModel
class MapViewModel @Inject constructor(
    cacheRepository: VideoCacheRepository,
    private val externalLinkLauncher: ExternalLinkLauncher,
) : ViewModel() {

    val locatedVideos: StateFlow<List<Video>> = cacheRepository.observeVideos()
        .map { videos -> videos.filter { it.hasLocation } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<Video?>(null)
    val selected: StateFlow<Video?> = _selected.asStateFlow()

    fun select(video: Video) {
        _selected.value = video
    }

    fun clearSelection() {
        _selected.value = null
    }

    fun openSelected() {
        _selected.value?.let { externalLinkLauncher.open(it.youtubeUrl) }
    }
}
