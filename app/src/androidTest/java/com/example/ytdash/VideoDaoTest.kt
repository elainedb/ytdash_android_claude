package com.example.ytdash

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ytdash.data.local.AppDatabase
import com.example.ytdash.data.local.VideoDao
import com.example.ytdash.data.local.toEntity
import com.example.ytdash.domain.Video
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: VideoDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.videoDao()
    }

    @After
    fun tearDown() = db.close()

    private fun video(id: String, lat: Double? = null, lng: Double? = null) =
        Video(id, "Title $id", "desc", "2026-01-01T00:00:00Z", "cronicas", "", lat, lng)

    @Test
    fun replaceAll_thenGetAll_roundTripsAndPreservesOrder() = runTest {
        val videos = listOf(video("a", 1.0, 2.0), video("b"), video("c"))
        dao.replaceAll(videos.mapIndexed { i, v -> v.toEntity(i) })

        val stored = dao.getAll()
        assertEquals(3, stored.size)
        assertEquals(listOf("a", "b", "c"), stored.map { it.id })
        assertEquals(1.0, stored.first().lat!!, 0.0001)
    }

    @Test
    fun replaceAll_replacesPreviousContents() = runTest {
        dao.replaceAll(listOf(video("old").toEntity(0)))
        dao.replaceAll(listOf(video("new1").toEntity(0), video("new2").toEntity(1)))

        val stored = dao.getAll()
        assertEquals(listOf("new1", "new2"), stored.map { it.id })
        assertEquals(2, dao.count())
    }
}
