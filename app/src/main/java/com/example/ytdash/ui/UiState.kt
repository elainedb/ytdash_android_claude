package com.example.ytdash.ui

import com.example.ytdash.data.model.Video
import com.example.ytdash.domain.SortMode

enum class Screen { LOGIN, HOME, MAP }

/** Explicit, observable list phase (constitution §1.3): loading / content / empty / error. */
enum class ListPhase { LOADING, CONTENT, EMPTY, ERROR }

data class AppUiState(
    val screen: Screen = Screen.LOGIN,
    val loginError: String? = null,
    val listPhase: ListPhase = ListPhase.LOADING,
    val videos: List<Video> = emptyList(),      // filtered + sorted, for display
    val mapMarkers: List<Video> = emptyList(),  // located videos (unfiltered)
    val totalCount: Int = 0,                    // total loaded videos (video_count)
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val sortMode: SortMode? = null,
    val filterPanelOpen: Boolean = false,
    val sortPanelOpen: Boolean = false,
    val selectedVideo: Video? = null,           // map detail bottom sheet target
    val externalUrl: String? = null,            // captured "open in YouTube" URL (test mode)
    val externalError: Boolean = false,         // real external launch failed
)
