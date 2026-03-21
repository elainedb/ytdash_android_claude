package dev.elainedb.ytdash_android_claude.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.elainedb.ytdash_android_claude.model.Video

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val description: String,
    val tags: String,
    val locationCity: String?,
    val locationCountry: String?,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val recordingDate: String?,
    val cacheTimestamp: Long = System.currentTimeMillis()
)

fun VideoEntity.toVideo(): Video {
    return Video(
        id = id,
        title = title,
        channelName = channelName,
        channelId = channelId,
        publishedAt = publishedAt,
        thumbnailUrl = thumbnailUrl,
        description = description,
        tags = if (tags.isNotEmpty()) tags.split(",") else emptyList(),
        locationCity = locationCity,
        locationCountry = locationCountry,
        locationLatitude = locationLatitude,
        locationLongitude = locationLongitude,
        recordingDate = recordingDate
    )
}

fun Video.toEntity(): VideoEntity {
    return VideoEntity(
        id = id,
        title = title,
        channelName = channelName,
        channelId = channelId,
        publishedAt = publishedAt,
        thumbnailUrl = thumbnailUrl,
        description = description,
        tags = tags.joinToString(","),
        locationCity = locationCity,
        locationCountry = locationCountry,
        locationLatitude = locationLatitude,
        locationLongitude = locationLongitude,
        recordingDate = recordingDate
    )
}
