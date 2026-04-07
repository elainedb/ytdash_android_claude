package dev.elainedb.ytdash_android_claude.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("""
        SELECT * FROM videos
        WHERE (:channelName IS NULL OR channelName = :channelName)
        AND (:country IS NULL OR locationCountry = :country)
        ORDER BY
            CASE WHEN :sortBy = 'published_desc' THEN publishedAt END DESC,
            CASE WHEN :sortBy = 'published_asc' THEN publishedAt END ASC,
            CASE WHEN :sortBy = 'recording_desc' THEN recordingDate END DESC,
            CASE WHEN :sortBy = 'recording_asc' THEN recordingDate END ASC
    """)
    fun getVideosWithFiltersAndSort(
        channelName: String?,
        country: String?,
        sortBy: String
    ): Flow<List<VideoEntity>>

    @Query("SELECT DISTINCT locationCountry FROM videos WHERE locationCountry IS NOT NULL ORDER BY locationCountry")
    fun getDistinctCountries(): Flow<List<String>>

    @Query("SELECT DISTINCT channelName FROM videos ORDER BY channelName")
    fun getDistinctChannels(): Flow<List<String>>

    @Query("SELECT * FROM videos WHERE locationLatitude IS NOT NULL AND locationLongitude IS NOT NULL")
    fun getVideosWithLocation(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE cacheTimestamp > :threshold LIMIT 1")
    suspend fun getVideosNewerThan(threshold: Long): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getTotalVideoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("DELETE FROM videos WHERE cacheTimestamp < :threshold")
    suspend fun deleteOldVideos(threshold: Long)
}
