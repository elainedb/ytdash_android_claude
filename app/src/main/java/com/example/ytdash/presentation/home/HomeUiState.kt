package com.example.ytdash.presentation.home

import com.example.ytdash.domain.model.SortOption
import com.example.ytdash.domain.model.Video

enum class PanelMode { NONE, FILTER, SORT }

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Content(
        val displayVideos: List<Video>,
        val totalCount: Int,
        // null = natural (as-loaded) order — no sort has been explicitly chosen yet.
        val sortOption: SortOption?,
        val filterCategory: String?,
        val availableCategories: List<String>,
        val panelMode: PanelMode,
    ) : HomeUiState
}
