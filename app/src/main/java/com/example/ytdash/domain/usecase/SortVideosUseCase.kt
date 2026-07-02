package com.example.ytdash.domain.usecase

import com.example.ytdash.domain.model.SortOption
import com.example.ytdash.domain.model.Video
import javax.inject.Inject

class SortVideosUseCase @Inject constructor() {
    operator fun invoke(videos: List<Video>, sortOption: SortOption): List<Video> = when (sortOption) {
        SortOption.DATE_NEWEST -> videos.sortedByDescending { it.publishedAt }
        SortOption.DATE_OLDEST -> videos.sortedBy { it.publishedAt }
        SortOption.TITLE_A_TO_Z -> videos.sortedBy { it.title.lowercase() }
        SortOption.TITLE_Z_TO_A -> videos.sortedByDescending { it.title.lowercase() }
    }
}
