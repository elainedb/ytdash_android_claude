package dev.elainedb.ytdash_android_claude.video.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.elainedb.ytdash_android_claude.config.ConfigHelper
import dev.elainedb.ytdash_android_claude.core.error.Failure
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.location.LocationUtils
import dev.elainedb.ytdash_android_claude.video.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.video.data.local.VideoEntity
import dev.elainedb.ytdash_android_claude.video.data.local.toVideo
import dev.elainedb.ytdash_android_claude.video.data.local.toEntity
import dev.elainedb.ytdash_android_claude.video.data.remote.YouTubeApiService
import dev.elainedb.ytdash_android_claude.video.data.remote.mergeWithDetails
import dev.elainedb.ytdash_android_claude.video.data.remote.toVideo
import dev.elainedb.ytdash_android_claude.video.domain.model.Video
import dev.elainedb.ytdash_android_claude.video.domain.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: YouTubeApiService,
    private val videoDao: VideoDao
) : VideoRepository {

    companion object {
        private const val CACHE_EXPIRY_HOURS = 24
    }

    override suspend fun getLatestVideos(
        channelIds: List<String>,
        forceRefresh: Boolean
    ): Result<List<Video>> {
        if (!forceRefresh) {
            val threshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
            val cachedVideos = videoDao.getVideosNewerThan(threshold)
            if (cachedVideos.isNotEmpty()) {
                return Result.Success(cachedVideos.map { it.toVideo() })
            }
        }

        return try {
            val apiKey = ConfigHelper.getYouTubeApiKey(context)
            val allVideos = fetchAllChannels(channelIds, apiKey)
            val enrichedVideos = enrichWithDetails(allVideos, apiKey)
            val geocodedVideos = geocodeVideos(enrichedVideos)

            videoDao.insertVideos(geocodedVideos.map { it.toEntity() })
            Result.Success(geocodedVideos)
        } catch (e: Exception) {
            val threshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
            val cachedVideos = videoDao.getVideosNewerThan(threshold)
            if (cachedVideos.isNotEmpty()) {
                Result.Success(cachedVideos.map { it.toVideo() })
            } else {
                Result.Error(Failure.Server("Failed to fetch videos: ${e.message}"))
            }
        }
    }

    private suspend fun fetchAllChannels(
        channelIds: List<String>,
        apiKey: String
    ): List<Video> = coroutineScope {
        channelIds.map { channelId ->
            async { fetchAllPagesForChannel(channelId, apiKey) }
        }.awaitAll().flatten()
    }

    private suspend fun fetchAllPagesForChannel(
        channelId: String,
        apiKey: String
    ): List<Video> {
        val allVideos = mutableListOf<Video>()
        var pageToken: String? = null

        do {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = pageToken,
                key = apiKey
            )
            allVideos.addAll(response.items.mapNotNull { item ->
                if (item.id.videoId != null) item.toVideo() else null
            })
            pageToken = response.nextPageToken
        } while (pageToken != null)

        return allVideos
    }

    private suspend fun enrichWithDetails(
        videos: List<Video>,
        apiKey: String
    ): List<Video> {
        if (videos.isEmpty()) return videos

        val videoMap = videos.associateBy { it.id }.toMutableMap()
        val videoIds = videos.map { it.id }

        videoIds.chunked(50).forEach { batch ->
            try {
                val response = apiService.getVideoDetails(
                    id = batch.joinToString(","),
                    key = apiKey
                )
                response.items.forEach { details ->
                    videoMap[details.id]?.let { video ->
                        videoMap[details.id] = video.mergeWithDetails(details)
                    }
                }
            } catch (_: Exception) {
                // Continue with unenriched videos
            }
        }

        return videoMap.values.toList()
    }

    private suspend fun geocodeVideos(videos: List<Video>): List<Video> {
        return videos.map { video ->
            if (video.locationLatitude != null && video.locationLongitude != null && video.locationCity == null) {
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

    override suspend fun getVideosByChannel(channelName: String): Result<List<Video>> {
        return try {
            val videos = videoDao.getVideosNewerThan(0)
                .filter { it.channelName == channelName }
                .map { it.toVideo() }
            Result.Success(videos)
        } catch (e: Exception) {
            Result.Error(Failure.Cache("Failed to query videos by channel: ${e.message}"))
        }
    }

    override suspend fun getVideosByCountry(country: String): Result<List<Video>> {
        return try {
            val videos = videoDao.getVideosNewerThan(0)
                .filter { it.locationCountry == country }
                .map { it.toVideo() }
            Result.Success(videos)
        } catch (e: Exception) {
            Result.Error(Failure.Cache("Failed to query videos by country: ${e.message}"))
        }
    }

    override suspend fun getTotalVideoCount(): Int {
        return videoDao.getTotalVideoCount()
    }
}
