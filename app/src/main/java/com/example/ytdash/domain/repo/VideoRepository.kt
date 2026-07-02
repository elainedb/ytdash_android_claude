package com.example.ytdash.domain.repo

import com.example.ytdash.domain.model.Video
import kotlinx.coroutines.flow.Flow

/**
 * Presentation depends on this abstraction, not a concrete data source (constitution §1.2).
 * [observeVideos] is backed by the local store (single source of truth, §1.5); [refresh] fetches
 * from the network and replaces the store. A failed refresh does not clear existing cached rows —
 * [observeVideos] keeps emitting the stale cache (constitution §1.5, AC-CACHE-01).
 */
interface VideoRepository {
    fun observeVideos(): Flow<List<Video>>
    suspend fun refresh(): Result<Unit>
}
