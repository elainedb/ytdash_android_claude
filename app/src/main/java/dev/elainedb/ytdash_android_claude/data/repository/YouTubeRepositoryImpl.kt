package dev.elainedb.ytdash_android_claude.data.repository

import android.content.Context
import dev.elainedb.ytdash_android_claude.core.error.Failure
import dev.elainedb.ytdash_android_claude.core.error.Result
import dev.elainedb.ytdash_android_claude.data.local.VideoDao
import dev.elainedb.ytdash_android_claude.data.local.toVideo
import dev.elainedb.ytdash_android_claude.data.remote.YouTubeApiService
import dev.elainedb.ytdash_android_claude.data.remote.mergeWithDetails
import dev.elainedb.ytdash_android_claude.data.remote.toVideo
import dev.elainedb.ytdash_android_claude.domain.model.Video
import dev.elainedb.ytdash_android_claude.domain.repository.VideoRepository
import dev.elainedb.ytdash_android_claude.data.local.toEntity
import dev.elainedb.ytdash_android_claude.utils.ConfigHelper
import dev.elainedb.ytdash_android_claude.utils.LocationUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    private val apiService: YouTubeApiService,
    private val videoDao: VideoDao,
    private val context: Context
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
                val threshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000)
                val cached = videoDao.getVideosNewerThan(threshold)
                if (cached.isNotEmpty()) {
                    val totalCount = videoDao.getTotalVideoCount()
                    if (totalCount > 0) {
                        return Result.Success(emptyList())
                    }
                }
            }
            refreshVideos(channelIds)
        } catch (e: Exception) {
            Result.Error(Failure.Network(e.message ?: "Failed to fetch videos"))
        }
    }

    override suspend fun refreshVideos(channelIds: List<String>): Result<List<Video>> {
        return try {
            val allVideos = coroutineScope {
                channelIds.map { channelId ->
                    async { fetchAllVideosFromChannel(channelId) }
                }.awaitAll().flatten()
            }

            val videosWithDetails = fetchVideoDetails(allVideos)
            val videosWithLocation = geocodeVideos(videosWithDetails)

            videoDao.insertVideos(videosWithLocation.map { it.toEntity() })

            Result.Success(videosWithLocation)
        } catch (e: Exception) {
            Result.Error(Failure.Server(e.message ?: "Failed to refresh videos"))
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
            videos.addAll(response.items.map { it.toVideo() })
            pageToken = response.nextPageToken
        } while (pageToken != null)

        return videos
    }

    private suspend fun fetchVideoDetails(videos: List<Video>): List<Video> {
        if (videos.isEmpty()) return videos

        val batches = videos.chunked(50)
        val detailsMap = coroutineScope {
            batches.map { batch ->
                async {
                    val ids = batch.joinToString(",") { it.id }
                    try {
                        apiService.getVideoDetails(id = ids, key = apiKey)
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .flatMap { it.items }
                .associateBy { it.id }
        }

        return videos.map { video ->
            detailsMap[video.id]?.let { video.mergeWithDetails(it) } ?: video
        }
    }

    private suspend fun geocodeVideos(videos: List<Video>): List<Video> {
        return coroutineScope {
            videos.map { video ->
                async {
                    if (video.locationLatitude != null && video.locationLongitude != null) {
                        val (city, country) = LocationUtils.reverseGeocode(
                            context, video.locationLatitude, video.locationLongitude
                        )
                        video.copy(locationCity = city, locationCountry = country)
                    } else {
                        video
                    }
                }
            }.awaitAll()
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
        return Result.Success(emptyList())
    }

    override suspend fun getVideosByCountry(country: String): Result<List<Video>> {
        return Result.Success(emptyList())
    }
}
