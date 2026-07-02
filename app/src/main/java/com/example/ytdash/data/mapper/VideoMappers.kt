package com.example.ytdash.data.mapper

import com.example.ytdash.data.local.VideoEntity
import com.example.ytdash.domain.model.GeoLocation
import com.example.ytdash.domain.model.Video
import java.time.Instant

fun VideoEntity.toDomain(): Video = Video(
    id = id,
    title = title,
    description = description,
    publishedAt = Instant.ofEpochMilli(publishedAtEpochMillis),
    category = category,
    thumbnailUrl = thumbnailUrl,
    location = if (lat != null && lng != null) GeoLocation(lat, lng) else null,
)

fun Video.toEntity(): VideoEntity = VideoEntity(
    id = id,
    title = title,
    description = description,
    publishedAtEpochMillis = publishedAt.toEpochMilli(),
    category = category,
    thumbnailUrl = thumbnailUrl,
    lat = location?.lat,
    lng = location?.lng,
)
