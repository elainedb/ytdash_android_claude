package dev.elainedb.ytdash_android_claude.domain.usecases

import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.core.usecases.UseCase
import dev.elainedb.ytdash_android_claude.domain.model.Video
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import javax.inject.Inject

data class GetVideosParams(
    val channelIds: List<String>,
    val forceRefresh: Boolean = false
)

class GetVideos @Inject constructor(
    private val videoRepository: VideoRepository
) : UseCase<List<Video>, GetVideosParams>() {

    override suspend fun invoke(params: GetVideosParams): Result<List<Video>> {
        return videoRepository.getLatestVideos(params.channelIds, params.forceRefresh)
    }
}
