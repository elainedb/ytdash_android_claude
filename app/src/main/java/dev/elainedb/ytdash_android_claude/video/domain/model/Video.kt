package dev.elainedb.ytdash_android_claude.video.domain.model

data class Video(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val description: String,
    val tags: List<String>,
    val locationCity: String?,
    val locationCountry: String?,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val recordingDate: String?
)
