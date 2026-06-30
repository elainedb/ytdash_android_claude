package com.example.ytdash.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ytdash.domain.Video

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val publishedAt: String,
    val category: String,
    val thumbnailUrl: String,
    val lat: Double?,
    val lng: Double?,
    /** Preserves API aggregation order across a process restart. */
    val position: Int,
)

fun VideoEntity.toDomain(): Video = Video(
    id = id,
    title = title,
    description = description,
    publishedAt = publishedAt,
    category = category,
    thumbnailUrl = thumbnailUrl,
    lat = lat,
    lng = lng,
)

fun Video.toEntity(position: Int): VideoEntity = VideoEntity(
    id = id,
    title = title,
    description = description,
    publishedAt = publishedAt,
    category = category,
    thumbnailUrl = thumbnailUrl,
    lat = lat,
    lng = lng,
    position = position,
)
