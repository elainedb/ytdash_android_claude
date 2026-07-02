package com.example.ytdash.data.remote

import com.example.ytdash.data.remote.dto.SearchListResponse
import com.example.ytdash.data.remote.dto.VideoListResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Mirrors the real YouTube Data API v3 signatures (spec/youtube-api.md) so the same client code
 * talks to the mock or the real API by swapping only the base URL + key at runtime.
 */
interface YouTubeApi {
    @GET("youtube/v3/search")
    suspend fun searchChannelVideos(
        @Query("channelId") channelId: String,
        @Query("part") part: String = "snippet",
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): SearchListResponse

    @GET("youtube/v3/videos")
    suspend fun getVideoDetails(
        @Query("id") ids: String,
        @Query("part") part: String = "snippet,contentDetails,recordingDetails",
    ): VideoListResponse
}
