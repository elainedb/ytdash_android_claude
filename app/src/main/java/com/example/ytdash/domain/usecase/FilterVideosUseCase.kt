package com.example.ytdash.domain.usecase

import com.example.ytdash.domain.model.Video
import javax.inject.Inject

/** `category` == null means "no filter applied" — all videos pass. */
class FilterVideosUseCase @Inject constructor() {
    operator fun invoke(videos: List<Video>, category: String?): List<Video> {
        if (category.isNullOrBlank()) return videos
        return videos.filter { it.category.equals(category, ignoreCase = true) }
    }
}
