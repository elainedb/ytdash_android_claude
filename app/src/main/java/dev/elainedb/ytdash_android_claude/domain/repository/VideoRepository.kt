package dev.elainedb.ytdash_android_claude.domain.repository

import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    suspend fun getLatestVideos(channelIds: List<String>, forceRefresh: Boolean): Result<List<Video>>
    suspend fun refreshVideos(channelIds: List<String>): Result<List<Video>>
    fun getVideosWithFiltersAndSort(channelName: String?, country: String?, sortBy: String): Flow<List<Video>>
    fun getDistinctCountries(): Flow<List<String>>
    fun getDistinctChannels(): Flow<List<String>>
    fun getVideosWithLocation(): Flow<List<Video>>
    suspend fun getTotalVideoCount(): Int
}
