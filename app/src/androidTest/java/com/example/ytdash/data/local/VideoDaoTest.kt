package com.example.ytdash.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Constitution §2: "at least one persistence test (cache read/write)". */
@RunWith(AndroidJUnit4::class)
class VideoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: VideoDao

    private fun entity(id: String, lat: Double? = null, lng: Double? = null) = VideoEntity(
        id = id,
        title = "Title $id",
        description = "Description $id",
        publishedAtEpochMillis = 1_700_000_000_000L,
        category = "tech",
        thumbnailUrl = "https://example.com/$id.jpg",
        lat = lat,
        lng = lng,
    )

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

    @Test
    fun writeThenReadReturnsSameData() = runTest {
        dao.insertAll(listOf(entity("1"), entity("2", lat = 48.8566, lng = 2.3522)))

        val all = dao.observeAll().first()

        assertThat(all).hasSize(2)
        assertThat(all.map { it.id }).containsExactly("1", "2")
        assertThat(all.first { it.id == "2" }.lat).isEqualTo(48.8566)
    }

    @Test
    fun replaceAllClearsPreviousDataBeforeInserting() = runTest {
        dao.insertAll(listOf(entity("1"), entity("2")))

        dao.replaceAll(listOf(entity("3")))

        val all = dao.observeAll().first()
        assertThat(all.map { it.id }).containsExactly("3")
    }

    @Test
    fun cacheSurvivesAsSourceOfTruthAcrossReads() = runTest {
        dao.insertAll(listOf(entity("1")))

        // Simulates AC-CACHE-01: a later read (e.g. after a failed network refresh) still
        // returns the previously cached rows, since nothing cleared them.
        val firstRead = dao.getAll()
        val secondRead = dao.getAll()

        assertThat(firstRead).isEqualTo(secondRead)
        assertThat(secondRead).hasSize(1)
    }
}
