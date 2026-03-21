package dev.elainedb.ytdash_android_claude.repository

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.elainedb.ytdash_android_claude.config.ConfigHelper
import dev.elainedb.ytdash_android_claude.database.VideoDao
import dev.elainedb.ytdash_android_claude.database.VideoDatabase
import dev.elainedb.ytdash_android_claude.database.toVideo
import dev.elainedb.ytdash_android_claude.database.toEntity
import dev.elainedb.ytdash_android_claude.model.Video
import dev.elainedb.ytdash_android_claude.model.mergeWithDetails
import dev.elainedb.ytdash_android_claude.model.toVideo
import dev.elainedb.ytdash_android_claude.network.YouTubeApiService
import dev.elainedb.ytdash_android_claude.utils.LocationUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.security.MessageDigest

class YouTubeRepository(private val context: Context) {

    companion object {
        private const val TAG = "YouTubeRepository"
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
        private const val MAX_PAGES = 5
        private const val CACHE_EXPIRY_HOURS = 24

        val CHANNEL_IDS = listOf(
            "UCynoa1DjwnvHAowA_jiMEAQ",
            "UCK0KOjX3beyB9nzonls0cuw",
            "UCACkIrvrGAQ7kuc0hMVwvmA",
            "UCtWRAKKvOEA0CXOue9BG8ZA"
        )
    }

    private val apiKey = ConfigHelper.getYouTubeApiKey(context)
    private val videoDao: VideoDao = VideoDatabase.getDatabase(context).videoDao()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val apiService: YouTubeApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Android-Package", context.packageName)
                .addHeader("X-Android-Cert", getSignature())
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YouTubeApiService::class.java)
    }

    private fun getSignature(): String {
        return try {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            val signatures = packageInfo.signatures
            if (signatures != null && signatures.isNotEmpty()) {
                val md = MessageDigest.getInstance("SHA1")
                md.update(signatures[0].toByteArray())
                md.digest().joinToString("") { "%02X".format(it) }
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun getLatestVideos(): List<Video> {
        val threshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
        val cachedVideos = videoDao.getVideosNewerThan(threshold)
        if (cachedVideos.isNotEmpty()) {
            Log.d(TAG, "Returning ${cachedVideos.size} cached videos")
            return cachedVideos.map { it.toVideo() }
        }
        return refreshVideos()
    }

    suspend fun refreshVideos(): List<Video> {
        return try {
            val videos = fetchAllChannels()
            val enrichedVideos = fetchVideoDetails(videos)
            val geolocatedVideos = reverseGeocodeVideos(enrichedVideos)
            videoDao.insertVideos(geolocatedVideos.map { it.toEntity() })
            Log.d(TAG, "Cached ${geolocatedVideos.size} videos")
            geolocatedVideos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch videos, falling back to cache", e)
            val cached = videoDao.getVideosNewerThan(0)
            if (cached.isNotEmpty()) {
                cached.map { it.toVideo() }
            } else {
                throw e
            }
        }
    }

    private suspend fun fetchAllChannels(): List<Video> = coroutineScope {
        CHANNEL_IDS.map { channelId ->
            async { fetchChannelVideos(channelId) }
        }.awaitAll().flatten()
    }

    private suspend fun fetchChannelVideos(channelId: String): List<Video> {
        val videos = mutableListOf<Video>()
        var pageToken: String? = null

        for (page in 0 until MAX_PAGES) {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = pageToken,
                key = apiKey
            )
            videos.addAll(response.items.mapNotNull { item ->
                if (item.id.videoId != null) item.toVideo() else null
            })
            pageToken = response.nextPageToken ?: break
        }

        Log.d(TAG, "Fetched ${videos.size} videos from channel $channelId")
        return videos
    }

    private suspend fun fetchVideoDetails(videos: List<Video>): List<Video> = coroutineScope {
        videos.chunked(50).map { batch ->
            async {
                val ids = batch.joinToString(",") { it.id }
                try {
                    val response = apiService.getVideoDetails(id = ids, key = apiKey)
                    val detailsMap = response.items.associateBy { it.id }
                    batch.map { video ->
                        val details = detailsMap[video.id]
                        if (details != null) video.mergeWithDetails(details) else video
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch details for batch", e)
                    batch
                }
            }
        }.awaitAll().flatten()
    }

    private suspend fun reverseGeocodeVideos(videos: List<Video>): List<Video> {
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

    fun getVideosWithFiltersAndSort(
        channelName: String?,
        country: String?,
        sortBy: String
    ): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(channelName, country, sortBy)
            .map { entities -> entities.map { it.toVideo() } }
    }

    fun getDistinctCountries(): Flow<List<String>> = videoDao.getDistinctCountries()

    fun getDistinctChannels(): Flow<List<String>> = videoDao.getDistinctChannels()

    fun getTotalVideoCount(): Flow<Int> = videoDao.getTotalVideoCount()

    suspend fun getVideosWithLocation(): List<Video> {
        return videoDao.getVideosWithLocation().map { it.toVideo() }
    }
}
