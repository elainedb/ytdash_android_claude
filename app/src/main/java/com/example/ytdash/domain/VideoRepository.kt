package com.example.ytdash.domain

import kotlinx.coroutines.flow.Flow

/** Result of a load: either videos (possibly served from cache) or a hard failure. */
sealed interface LoadResult {
    data class Success(val videos: List<Video>, val fromCache: Boolean) : LoadResult
    data class Failure(val message: String) : LoadResult
}

/**
 * Presentation depends on this abstraction, not on Retrofit/Room (dependency inversion).
 * The local store is the single source of truth the UI reads from; the network refreshes it.
 *
 * Emits progressively: the searched list first (so it renders immediately), then the same list
 * enriched with map locations. On network failure it falls back to the most recent cache.
 */
interface VideoRepository {
    fun loadVideos(): Flow<LoadResult>
}
