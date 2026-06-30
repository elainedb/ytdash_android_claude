package com.example.ytdash.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ytdash.data.VideoRepository
import com.example.ytdash.data.model.Video
import com.example.ytdash.domain.SortOrder
import com.example.ytdash.domain.VideoQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val all: List<Video>,
        val visible: List<Video>,
        val categories: List<String>,
        val activeCategory: String?,
        val sort: SortOrder,
        val fromCache: Boolean,
    ) : HomeUiState

    data object Empty : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: VideoRepository) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var all: List<Video> = emptyList()
    private var activeCategory: String? = null
    private var sort: SortOrder = SortOrder.DATE_DESC

    init {
        load(forceRefresh = false)
    }

    fun load(forceRefresh: Boolean) {
        _state.value = HomeUiState.Loading
        viewModelScope.launch {
            val result = repository.getVideos(forceRefresh)
            result.fold(
                onSuccess = { load ->
                    all = load.videos
                    if (all.isEmpty()) {
                        _state.value = HomeUiState.Empty
                    } else {
                        recompute(load.fromCache)
                    }
                },
                onFailure = { e ->
                    _state.value = HomeUiState.Error(e.message ?: "Failed to load videos")
                },
            )
        }
    }

    fun setCategory(category: String?) {
        activeCategory = category
        if (all.isNotEmpty()) recompute(currentFromCache())
    }

    fun setSort(order: SortOrder) {
        sort = order
        if (all.isNotEmpty()) recompute(currentFromCache())
    }

    private fun currentFromCache(): Boolean =
        (_state.value as? HomeUiState.Content)?.fromCache ?: false

    private fun recompute(fromCache: Boolean) {
        val visible = VideoQuery.apply(all, activeCategory, sort)
        _state.value = HomeUiState.Content(
            all = all,
            visible = visible,
            categories = VideoQuery.categories(all),
            activeCategory = activeCategory,
            sort = sort,
            fromCache = fromCache,
        )
    }
}
