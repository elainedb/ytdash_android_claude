package com.example.ytdash.data.repo

import com.example.ytdash.config.Channel
import com.example.ytdash.data.local.VideoDao
import com.example.ytdash.data.local.toDomain
import com.example.ytdash.data.local.toEntity
import com.example.ytdash.data.remote.RemoteDataSource
import com.example.ytdash.domain.LoadResult
import com.example.ytdash.domain.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Network refreshes the local store, which is the single source of truth the UI reads from.
 * Emits the searched list first (fast first paint), then the location-enriched list. On a network
 * failure we fall back to the most recently cached videos (no blocking error) and only surface a
 * hard failure when there is nothing cached to show.
 */
class VideoRepositoryImpl(
    private val remote: RemoteDataSource,
    private val dao: VideoDao,
    private val channels: List<Channel>,
) : VideoRepository {

    override fun loadVideos(): Flow<LoadResult> = flow {
        try {
            val base = remote.fetchSearch(channels)
            if (base.isNotEmpty()) {
                dao.replaceAll(base.mapIndexed { index, v -> v.toEntity(index) })
                emit(LoadResult.Success(base, fromCache = false))

                val enriched = remote.enrich(base)
                dao.replaceAll(enriched.mapIndexed { index, v -> v.toEntity(index) })
                emit(LoadResult.Success(enriched, fromCache = false))
            } else {
                emit(cacheOrFailure("No videos available"))
            }
        } catch (e: Exception) {
            emit(cacheOrFailure(e.message ?: "Unable to load videos"))
        }
    }

    private suspend fun cacheOrFailure(message: String): LoadResult {
        val cached = dao.getAll().map { it.toDomain() }
        return if (cached.isNotEmpty()) LoadResult.Success(cached, fromCache = true)
        else LoadResult.Failure(message)
    }
}
