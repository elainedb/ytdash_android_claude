package dev.elainedb.ytdash_android_claude.domain.usecases

import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import dev.elainedb.ytdash_android_claude.domain.model.Video
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetVideosByCountry @Inject constructor(
    private val videoRepository: VideoRepository
) : UseCase<List<Video>, String>() {

    override suspend fun invoke(params: String): Result<List<Video>> {
        return try {
            val videos = videoRepository.getVideosWithFiltersAndSort(null, params, "publishedAt_desc").first()
            Result.Success(videos)
        } catch (e: Exception) {
            Result.Error(dev.elainedb.ytdash_android_claude.core.error.Failure.Cache("Failed to get videos by country: ${e.message}"))
        }
    }
}
