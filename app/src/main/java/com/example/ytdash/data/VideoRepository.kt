package com.example.ytdash.data

import com.example.ytdash.data.local.ChannelConfig
import com.example.ytdash.data.local.SourceChannel
import com.example.ytdash.data.local.VideoCache
import com.example.ytdash.data.model.Video
import com.example.ytdash.data.remote.YouTubeApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Outcome of a load: the videos, and whether they came from the stale cache (network was down). */
data class VideoLoad(val videos: List<Video>, val fromCache: Boolean)

interface VideoRepository {
    /**
     * Aggregates ALL configured channels, follows pagination, merges/dedupes by videoId, and
     * enriches with location via videos.list. On network failure, falls back to the disk cache if
     * present (stale-fallback). Returns failure only when there is no network result AND no cache.
     */
    suspend fun getVideos(forceRefresh: Boolean = false): Result<VideoLoad>

    /** Last successfully loaded list held in memory (for cheap cross-screen reuse), or null. */
    fun cachedInMemory(): List<Video>?
}

class VideoRepositoryImpl(
    private val api: YouTubeApi,
    private val cache: VideoCache,
    private val channels: List<SourceChannel>,
) : VideoRepository {

    @Volatile
    private var memory: List<Video>? = null
    private val loadMutex = Mutex()

    override fun cachedInMemory(): List<Video>? = memory

    override suspend fun getVideos(forceRefresh: Boolean): Result<VideoLoad> = loadMutex.withLock {
        if (!forceRefresh) {
            memory?.let { return Result.success(VideoLoad(it, fromCache = false)) }
        }
        try {
            val fresh = fetchFromNetwork()
            cache.save(fresh)
            memory = fresh
            Result.success(VideoLoad(fresh, fromCache = false))
        } catch (e: Exception) {
            // Network/parse failure → fall back to the persisted cache if we have one.
            val cached = memory ?: cache.load().takeIf { it.isNotEmpty() }
            if (!cached.isNullOrEmpty()) {
                memory = cached
                Result.success(VideoLoad(cached, fromCache = true))
            } else {
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchFromNetwork(): List<Video> {
        if (channels.isEmpty()) throw IllegalStateException("No source channels configured")

        // 1) Per channel: paginate search.list; remember which channel (label) each video came from.
        val ordered = LinkedHashMap<String, Video>() // videoId -> partial Video, dedup, insertion order
        val labelOf = LinkedHashMap<String, String>()
        for (channel in channels) {
            val items = api.searchAllPages(channel.id)
            for (item in items) {
                val id = item.id?.videoId ?: item.snippet?.resourceId?.videoId ?: continue
                if (ordered.containsKey(id)) continue // dedupe; first channel wins the label
                val s = item.snippet
                ordered[id] = Video(
                    id = id,
                    title = s?.title.orEmpty(),
                    description = s?.description.orEmpty(),
                    publishedAt = s?.publishedAt.orEmpty(),
                    category = channel.label,
                    thumbnailUrl = s?.thumbnails?.bestUrl(),
                    lat = null,
                    lng = null,
                )
                labelOf[id] = channel.label
            }
        }
        if (ordered.isEmpty()) return emptyList()

        // 2) videos.list for locations (and to backfill any missing snippet fields).
        val details = api.videoDetails(ordered.keys.toList())
        for (d in details) {
            val id = d.id ?: continue
            val base = ordered[id] ?: continue
            val loc = d.recordingDetails?.location
            ordered[id] = base.copy(
                title = base.title.ifBlank { d.snippet?.title.orEmpty() },
                description = base.description.ifBlank { d.snippet?.description.orEmpty() },
                publishedAt = base.publishedAt.ifBlank { d.snippet?.publishedAt.orEmpty() },
                thumbnailUrl = base.thumbnailUrl ?: d.snippet?.thumbnails?.bestUrl(),
                lat = loc?.latitude,
                lng = loc?.longitude,
            )
        }
        return ordered.values.toList()
    }
}
