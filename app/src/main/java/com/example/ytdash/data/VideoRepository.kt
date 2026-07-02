package com.example.ytdash.data

import com.example.ytdash.data.cache.VideoCache
import com.example.ytdash.data.model.SourceChannel
import com.example.ytdash.data.model.Video
import com.example.ytdash.data.remote.YouTubeApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface RefreshOutcome {
    /** Network fetch succeeded; [videos] are fresh. */
    data class Success(val videos: List<Video>) : RefreshOutcome
    /** Network failed but a non-empty cache was served instead (no blocking error). */
    data class StaleFallback(val videos: List<Video>) : RefreshOutcome
    /** Network failed and there was nothing cached to fall back to. */
    data class Failure(val error: Throwable) : RefreshOutcome
}

interface VideoRepository {
    /** The local store is the single source of truth the UI observes. */
    val videos: StateFlow<List<Video>>
    fun hydrateFromCache()
    suspend fun refresh(): RefreshOutcome
}

class DefaultVideoRepository(
    private val channels: List<SourceChannel>,
    private val api: YouTubeApi,
    private val cache: VideoCache,
) : VideoRepository {

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    override val videos: StateFlow<List<Video>> = _videos.asStateFlow()

    override fun hydrateFromCache() {
        val cached = cache.load()
        if (cached.isNotEmpty()) _videos.value = cached
    }

    override suspend fun refresh(): RefreshOutcome {
        return try {
            val merged = aggregate()
            cache.save(merged)
            _videos.value = merged
            RefreshOutcome.Success(merged)
        } catch (e: Exception) {
            android.util.Log.w("ytdash", "refresh failed: ${e.javaClass.simpleName}: ${e.message}", e)
            val cached = cache.load()
            if (cached.isNotEmpty()) {
                _videos.value = cached
                RefreshOutcome.StaleFallback(cached)
            } else {
                RefreshOutcome.Failure(e)
            }
        }
    }

    /** Iterate every configured channel, follow pagination, dedupe by id, enrich with location. */
    private suspend fun aggregate(): List<Video> {
        val byId = LinkedHashMap<String, Video>()
        for (ch in channels) {
            // Prefer the cheap playlistItems idiom (1 unit) over search.list (100 units); fall back
            // to search only when the channel exposes no uploads playlist. Network errors propagate
            // so refresh() can serve the cached list instead (AC-CACHE-01).
            val uploads = api.channelUploadsPlaylist(ch.id)
            val items = if (uploads != null) {
                api.playlistItemsAllPages(uploads)
            } else {
                api.searchChannelAllPages(ch.id)
            }
            for (item in items) {
                val vid = item.id?.videoId ?: item.snippet?.resourceId?.videoId ?: continue
                if (byId.containsKey(vid)) continue
                val sn = item.snippet
                byId[vid] = Video(
                    id = vid,
                    title = sn?.title.orEmpty(),
                    description = sn?.description.orEmpty(),
                    publishedAt = sn?.publishedAt.orEmpty(),
                    category = ch.label, // "category" == source channel label (youtube-api.md)
                    thumbnailUrl = sn?.thumbnails?.medium?.url
                        ?: sn?.thumbnails?.high?.url
                        ?: sn?.thumbnails?.default?.url.orEmpty(),
                )
            }
        }
        // Enrich with recordingDetails.location (the map markers) via videos.list. Location is an
        // enhancement — if only this step fails, keep the videos we already have (no stale fallback).
        if (byId.isNotEmpty()) {
            try {
                val details = api.videosDetails(byId.keys.toList())
                for (d in details) {
                    val loc = d.recordingDetails?.location ?: continue
                    val lat = loc.latitude ?: continue
                    val lng = loc.longitude ?: continue
                    byId[d.id]?.let { byId[d.id] = it.copy(lat = lat, lng = lng) }
                }
            } catch (_: Exception) {
                // keep byId as-is (no locations)
            }
        }
        return byId.values.toList()
    }
}
