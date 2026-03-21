package dev.elainedb.ytdash_android_claude.repository

import android.content.Context
import android.content.pm.PackageManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.elainedb.ytdash_android_claude.ConfigHelper
import dev.elainedb.ytdash_android_claude.database.VideoDao
import dev.elainedb.ytdash_android_claude.database.VideoDatabase
import dev.elainedb.ytdash_android_claude.database.toEntity
import dev.elainedb.ytdash_android_claude.database.toVideo
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
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
        private const val CACHE_EXPIRY_HOURS = 24
        private const val MAX_PAGES_PER_CHANNEL = 5
        private const val MAX_RESULTS_PER_PAGE = 50

        private val CHANNEL_IDS = listOf(
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
                .addHeader("X-Android-Cert", getSha1Signature())
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

    private fun getSha1Signature(): String {
        return try {
            @Suppress("DEPRECATION")
            val signatures = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(signatures?.get(0)?.toByteArray() ?: return "")
            digest.joinToString(":") { "%02X".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun getLatestVideos(): List<Video> {
        val cacheThreshold = System.currentTimeMillis() - (CACHE_EXPIRY_HOURS * 60 * 60 * 1000)
        val cachedVideos = videoDao.getVideosNewerThan(cacheThreshold)

        if (cachedVideos.isNotEmpty()) {
            return cachedVideos.map { it.toVideo() }
        }

        return refreshVideos()
    }

    suspend fun refreshVideos(): List<Video> {
        val videos = fetchAllChannels()
        val enrichedVideos = fetchVideoDetails(videos)
        val geolocatedVideos = reverseGeocodeVideos(enrichedVideos)

        videoDao.insertVideos(geolocatedVideos.map { it.toEntity() })

        return geolocatedVideos
    }

    private suspend fun fetchAllChannels(): List<Video> = coroutineScope {
        CHANNEL_IDS.map { channelId ->
            async { fetchChannelVideos(channelId) }
        }.awaitAll().flatten()
    }

    private suspend fun fetchChannelVideos(channelId: String): List<Video> {
        val videos = mutableListOf<Video>()
        var nextPageToken: String? = null

        for (page in 0 until MAX_PAGES_PER_CHANNEL) {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = nextPageToken,
                key = apiKey
            )

            videos.addAll(response.items.map { it.toVideo() })

            nextPageToken = response.nextPageToken
            if (nextPageToken == null) break
        }

        return videos
    }

    private suspend fun fetchVideoDetails(videos: List<Video>): List<Video> = coroutineScope {
        videos.chunked(MAX_RESULTS_PER_PAGE).map { batch ->
            async {
                val ids = batch.joinToString(",") { it.id }
                val detailsResponse = apiService.getVideoDetails(id = ids, key = apiKey)

                batch.map { video ->
                    val details = detailsResponse.items.find { it.id == video.id }
                    if (details != null) video.mergeWithDetails(details) else video
                }
            }
        }.awaitAll().flatten()
    }

    private suspend fun reverseGeocodeVideos(videos: List<Video>): List<Video> {
        return videos.map { video ->
            if (video.locationLatitude != null && video.locationLongitude != null) {
                val (city, country) = LocationUtils.reverseGeocode(
                    context, video.locationLatitude, video.locationLongitude
                )
                video.copy(locationCity = city, locationCountry = country)
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
}
