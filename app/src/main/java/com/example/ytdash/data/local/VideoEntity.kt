package com.example.ytdash.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val publishedAtEpochMillis: Long,
    val category: String,
    val thumbnailUrl: String,
    val lat: Double?,
    val lng: Double?,
)
