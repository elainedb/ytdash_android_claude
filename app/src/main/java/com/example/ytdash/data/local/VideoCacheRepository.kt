package com.example.ytdash.data.local

import androidx.room.withTransaction
import com.example.ytdash.data.remote.YouTubeRepository
import com.example.ytdash.domain.model.Video
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth (constitution §1.5): the UI (list/map/filter/sort) reads only from
 * Room. `refresh()` fetches the network and replaces the table on success; on failure the
 * existing rows are left untouched (stale-cache fallback, AC-CACHE-01) — the caller decides
 * whether to surface the error (only when there's nothing cached to fall back to).
 */
@Singleton
class VideoCacheRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: VideoDao,
    private val remote: YouTubeRepository,
) {
    fun observeVideos(): Flow<List<Video>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun refresh(): Result<Unit> = runCatching {
        val videos = remote.fetchAllVideos()
        db.withTransaction {
            dao.clearAll()
            dao.insertAll(videos.map { it.toEntity() })
        }
    }
}
