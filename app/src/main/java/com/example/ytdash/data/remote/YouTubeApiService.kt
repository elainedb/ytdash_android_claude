package com.example.ytdash.data.remote

import com.example.ytdash.data.remote.dto.SearchListResponseDto
import com.example.ytdash.data.remote.dto.VideoListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Mirrors the real YouTube Data API v3 signatures (spec/youtube-api.md) so the mock ↔ real swap
 * is a runtime base-URL/key change only. The API key is injected by an OkHttp interceptor (see
 * di/NetworkModule), not as an explicit @Query param here, so it always reflects the current
 * RuntimeConfig without needing to rebuild this interface.
 */
interface YouTubeApiService {

    @GET("youtube/v3/search")
    suspend fun search(
        @Query("channelId") channelId: String,
        @Query("part") part: String = "snippet",
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): SearchListResponseDto

    @GET("youtube/v3/videos")
    suspend fun videos(
        @Query("id") ids: String,
        @Query("part") part: String = "snippet,contentDetails,recordingDetails",
    ): VideoListResponseDto
}
