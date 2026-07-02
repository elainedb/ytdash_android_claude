package com.example.ytdash.domain.model

import java.time.Instant

/**
 * The app's domain representation of a video, assembled from the YouTube Data API's
 * search.list (snippet) + videos.list (recordingDetails.location) responses — see
 * spec/youtube-api.md mapping table. `category` is the SOURCE CHANNEL's label (the API's
 * `snippet.channelTitle`), not YouTube's numeric categoryId.
 */
data class Video(
    val id: String,
    val title: String,
    val description: String,
    val publishedAt: Instant,
    val category: String,
    val thumbnailUrl: String,
    val location: GeoLocation?,
) {
    val youtubeUrl: String
        get() = "https://www.youtube.com/watch?v=$id"
}
