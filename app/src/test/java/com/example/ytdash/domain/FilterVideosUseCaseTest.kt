package com.example.ytdash.domain

import com.example.ytdash.domain.model.Video
import com.example.ytdash.domain.usecase.FilterVideosUseCase
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class FilterVideosUseCaseTest {

    private val useCase = FilterVideosUseCase()

    private fun video(id: String, category: String) = Video(
        id = id,
        title = "Video $id",
        description = "",
        publishedAt = Instant.EPOCH,
        category = category,
        thumbnailUrl = "",
        location = null,
    )

    private val videos = listOf(
        video("1", "tech"),
        video("2", "tech"),
        video("3", "music"),
        video("4", "news"),
    )

    @Test
    fun `null category returns all videos unfiltered`() {
        assertThat(useCase(videos, null)).hasSize(4)
    }

    @Test
    fun `blank category returns all videos unfiltered`() {
        assertThat(useCase(videos, "  ")).hasSize(4)
    }

    @Test
    fun `filters to only the matching category`() {
        val result = useCase(videos, "tech")
        assertThat(result.map { it.id }).containsExactly("1", "2")
    }

    @Test
    fun `filter comparison is case-insensitive`() {
        val result = useCase(videos, "TECH")
        assertThat(result.map { it.id }).containsExactly("1", "2")
    }

    @Test
    fun `category with no matches returns empty list`() {
        assertThat(useCase(videos, "sports")).isEmpty()
    }
}
