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

    @Query("SELECT * FROM videos")
    suspend fun getAll(): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VideoEntity>)

    @Query("DELETE FROM videos")
    suspend fun clearAll()

    /** Replace-on-refresh: the network response fully replaces the cached set (constitution §1.5). */
    @Transaction
    suspend fun replaceAll(items: List<VideoEntity>) {
        clearAll()
        insertAll(items)
    }
}
