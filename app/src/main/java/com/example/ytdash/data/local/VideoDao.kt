package com.example.ytdash.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY position ASC")
    suspend fun getAll(): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM videos")
    suspend fun clear()

    /** Replace-on-refresh: the freshly fetched set becomes the source of truth. */
    @Transaction
    suspend fun replaceAll(videos: List<VideoEntity>) {
        clear()
        insertAll(videos)
    }
}
