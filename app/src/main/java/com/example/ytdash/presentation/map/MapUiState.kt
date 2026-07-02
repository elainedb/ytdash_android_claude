package com.example.ytdash.presentation.map

import com.example.ytdash.domain.model.Video

data class MapUiState(
    val locatedVideos: List<Video> = emptyList(),
    val selectedVideo: Video? = null,
)
