package dev.elainedb.ytdash_android_claude.video.domain.usecase

import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import dev.elainedb.ytdash_android_claude.video.domain.model.Video
import dev.elainedb.ytdash_android_claude.video.domain.repository.VideoRepository
import javax.inject.Inject

class GetVideosByChannel @Inject constructor(
    private val videoRepository: VideoRepository
) : UseCase<List<Video>, String>() {

    override suspend fun invoke(params: String): Result<List<Video>> {
        return videoRepository.getVideosByChannel(params)
    }
}
