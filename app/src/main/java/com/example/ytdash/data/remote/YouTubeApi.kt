package com.example.ytdash.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/** Mirrors the YouTube Data API v3 signatures (see spec/youtube-api.md). */
interface YouTubeApi {

    @GET("search")
    suspend fun search(
        @Query("key") key: String,
        @Query("channelId") channelId: String,
        @Query("part") part: String = "snippet",
        @Query("order") order: String = "date",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): SearchResponse

    @GET("videos")
    suspend fun videos(
        @Query("key") key: String,
        @Query("id") ids: String,
        @Query("part") part: String = "snippet,contentDetails,recordingDetails",
    ): VideosResponse
}
