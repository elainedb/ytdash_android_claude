package dev.elainedb.ytdash_android_claude.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.elainedb.ytdash_android_claude.config.ConfigHelper
import dev.elainedb.ytdash_android_claude.core.error.Failure
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.data.local.toVideo
import dev.elainedb.ytdash_android_claude.data.local.toEntity
import dev.elainedb.ytdash_android_claude.data.location.LocationUtils
import dev.elainedb.ytdash_android_claude.data.remote.YouTubeApiService
import dev.elainedb.ytdash_android_claude.data.remote.mergeWithDetails
import dev.elainedb.ytdash_android_claude.data.remote.toVideo
import dev.elainedb.ytdash_android_claude.domain.model.Video
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class YouTubeRepositoryImpl @Inject constructor(
    private val apiService: YouTubeApiService,
    private val videoDao: VideoDao,
    @ApplicationContext private val context: Context
) : VideoRepository {

    companion object {
        private const val CACHE_EXPIRY_HOURS = 24
    }

    private val apiKey: String by lazy { ConfigHelper.getYouTubeApiKey(context) }

    override suspend fun getLatestVideos(
        channelIds: List<String>,
        forceRefresh: Boolean
    ): Result<List<Video>> {
        return try {
            if (!forceRefresh) {
                val threshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
                val cachedVideos = videoDao.getVideosNewerThan(threshold)
                if (cachedVideos.isNotEmpty()) {
                    return Result.Success(cachedVideos.map { it.toVideo() })
                }
            }
            refreshVideos(channelIds)
        } catch (e: Exception) {
            // Fallback to cache on error
            try {
                val threshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
                val cachedVideos = videoDao.getVideosNewerThan(threshold)
                if (cachedVideos.isNotEmpty()) {
                    Result.Success(cachedVideos.map { it.toVideo() })
                } else {
                    Result.Error(Failure.Network("Failed to fetch videos: ${e.message}"))
                }
            } catch (cacheError: Exception) {
                Result.Error(Failure.Network("Failed to fetch videos: ${e.message}"))
            }
        }
    }

    override suspend fun refreshVideos(channelIds: List<String>): Result<List<Video>> {
        return try {
            val allVideos = coroutineScope {
                channelIds.map { channelId ->
                    async { fetchAllVideosFromChannel(channelId) }
                }.awaitAll().flatten()
            }

            // Fetch details in batches of 50
            val enrichedVideos = fetchVideoDetails(allVideos)

            // Reverse geocode videos with coordinates
            val geocodedVideos = geocodeVideos(enrichedVideos)

            // Cache results
            videoDao.insertVideos(geocodedVideos.map { it.toEntity() })

            Result.Success(geocodedVideos)
        } catch (e: Exception) {
            Result.Error(Failure.Server("Failed to refresh videos: ${e.message}"))
        }
    }

    private suspend fun fetchAllVideosFromChannel(channelId: String): List<Video> {
        val videos = mutableListOf<Video>()
        var pageToken: String? = null

        do {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = pageToken,
                key = apiKey
            )
            videos.addAll(response.items.mapNotNull { item ->
                if (item.id.videoId != null) item.toVideo() else null
            })
            pageToken = response.nextPageToken
        } while (pageToken != null)

        return videos
    }

    private suspend fun fetchVideoDetails(videos: List<Video>): List<Video> {
        if (videos.isEmpty()) return videos

        val videosById = videos.associateBy { it.id }
        val enrichedVideos = videos.toMutableList()

        videos.chunked(50).forEach { batch ->
            try {
                val ids = batch.joinToString(",") { it.id }
                val response = apiService.getVideoDetails(id = ids, key = apiKey)

                response.items.forEach { details ->
                    val index = enrichedVideos.indexOfFirst { it.id == details.id }
                    if (index >= 0) {
                        enrichedVideos[index] = enrichedVideos[index].mergeWithDetails(details)
                    }
                }
            } catch (_: Exception) {
                // Continue with undetailed videos
            }
        }

        return enrichedVideos
    }

    private suspend fun geocodeVideos(videos: List<Video>): List<Video> {
        return videos.map { video ->
            if (video.locationLatitude != null && video.locationLongitude != null) {
                try {
                    val (city, country) = LocationUtils.reverseGeocode(
                        context, video.locationLatitude, video.locationLongitude
                    )
                    video.copy(locationCity = city, locationCountry = country)
                } catch (_: Exception) {
                    video
                }
            } else {
                video
            }
        }
    }

    override fun getVideosWithFiltersAndSort(
        channelName: String?,
        country: String?,
        sortBy: String
    ): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(channelName, country, sortBy)
            .map { entities -> entities.map { it.toVideo() } }
    }

    override fun getDistinctCountries(): Flow<List<String>> {
        return videoDao.getDistinctCountries()
    }

    override fun getDistinctChannels(): Flow<List<String>> {
        return videoDao.getDistinctChannels()
    }

    override fun getVideosWithLocation(): Flow<List<Video>> {
        return videoDao.getVideosWithLocation()
            .map { entities -> entities.map { it.toVideo() } }
    }

    override suspend fun getTotalVideoCount(): Int {
        return videoDao.getTotalVideoCount()
    }
}
