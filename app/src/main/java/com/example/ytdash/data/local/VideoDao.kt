package com.example.ytdash.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun observeAll(): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(videos: List<VideoEntity>)

    @Query("DELETE FROM videos")
    suspend fun clear()

    /** Replace-on-refresh (constitution §1.5): the network refresh is the sole writer. */
    @Transaction
    suspend fun replaceAll(videos: List<VideoEntity>) {
        clear()
        insertAll(videos)
    }
}
