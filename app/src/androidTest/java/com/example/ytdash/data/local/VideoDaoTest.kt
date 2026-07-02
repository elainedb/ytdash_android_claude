package com.example.ytdash.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Persistence test for the cache read/write contract (constitution §2). */
@RunWith(AndroidJUnit4::class)
class VideoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: VideoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.videoDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(id: String) = VideoEntity(
        id = id,
        title = "Title $id",
        description = "desc",
        publishedAt = "2026-01-01T00:00:00Z",
        category = "tech",
        thumbnailUrl = "https://example.com/$id.jpg",
        lat = null,
        lng = null,
    )

    @Test
    fun insertAndReadBack() = runBlocking {
        dao.insertAll(listOf(entity("1"), entity("2")))
        val all = dao.observeAll().first()
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "1" })
    }

    @Test
    fun clearAllThenInsertReplacesContents() = runBlocking {
        dao.insertAll(listOf(entity("1"), entity("2")))
        dao.clearAll()
        dao.insertAll(listOf(entity("3")))
        val all = dao.observeAll().first()
        assertEquals(listOf("3"), all.map { it.id })
    }
}
