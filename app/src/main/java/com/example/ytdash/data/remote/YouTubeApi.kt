package com.example.ytdash.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Mirrors spec/youtube-api.md. Note there is no catch-all/"all channels" query — callers must
 * iterate configured channels themselves (see [com.example.ytdash.data.repo.VideoAggregator]).
 * The `key` query param is injected by [com.example.ytdash.data.remote.RuntimeConfigInterceptor]
 * at request time, not declared here, so the runtime `apiKey` override (constitution §4) applies
 * uniformly to every call without threading it through every method signature.
 */
interface YouTubeApi {
    @GET("youtube/v3/search")
    suspend fun search(
        @Query("channelId") channelId: String,
        @Query("part") part: String = "snippet",
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): SearchListResponse

    @GET("youtube/v3/videos")
    suspend fun videos(
        @Query("id") ids: String,
        @Query("part") part: String = "snippet,contentDetails,recordingDetails",
    ): VideoListResponse
}
