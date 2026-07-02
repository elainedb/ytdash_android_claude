package com.example.ytdash.data.repo

import com.example.ytdash.data.local.VideoDao
import com.example.ytdash.data.local.VideoEntity
import com.example.ytdash.data.remote.ChannelConfigReader
import com.example.ytdash.domain.model.Video
import com.example.ytdash.domain.repo.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val aggregator: VideoAggregator,
    private val dao: VideoDao,
    private val channelConfigReader: ChannelConfigReader,
) : VideoRepository {

    override fun observeVideos(): Flow<List<Video>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val channels = channelConfigReader.loadChannels()
            val aggregated = aggregator.fetchAll(channels)
            dao.replaceAll(aggregated.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            // Constitution §1.5/§1.6: a failed refresh must NOT clear the existing cache — the
            // Flow from Room keeps emitting whatever was already stored (AC-CACHE-01).
            Result.failure(e)
        }
    }
}

private fun VideoEntity.toDomain() = Video(
    id = id,
    title = title,
    description = description,
    publishedAt = publishedAt,
    category = category,
    thumbnailUrl = thumbnailUrl,
    lat = lat,
    lng = lng,
)

private fun AggregatedVideo.toEntity() = VideoEntity(
    id = id,
    title = title,
    description = description,
    publishedAt = publishedAt,
    category = category,
    thumbnailUrl = thumbnailUrl,
    lat = lat,
    lng = lng,
)
