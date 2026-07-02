package com.example.ytdash.domain.repository

import com.example.ytdash.domain.model.Video
import kotlinx.coroutines.flow.Flow

/**
 * The local store is the single source of truth (constitution §1.5): [observeVideos] always
 * reads from it, [refresh] fetches the network and replaces the store on success. On network
 * failure, refresh fails but the store — and therefore the UI — keeps showing the last good data.
 */
interface VideoRepository {
    fun observeVideos(): Flow<List<Video>>
    suspend fun refresh(): Result<Unit>
}
