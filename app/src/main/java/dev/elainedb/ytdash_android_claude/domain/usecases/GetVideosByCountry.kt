package dev.elainedb.ytdash_android_claude.domain.usecases

import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import dev.elainedb.ytdash_android_claude.domain.model.Video
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import javax.inject.Inject

class GetVideosByCountry @Inject constructor(
    private val videoRepository: VideoRepository
) : UseCase<List<Video>, String>() {

    override suspend fun invoke(params: String): Result<List<Video>> {
        return videoRepository.getVideosByCountry(params)
    }
}
