package com.example.ytdash.domain

import com.example.ytdash.domain.model.SortOption
import com.example.ytdash.domain.model.Video
import com.example.ytdash.domain.usecase.SortVideosUseCase
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class SortVideosUseCaseTest {

    private val useCase = SortVideosUseCase()

    private fun video(id: String, title: String, publishedAt: String) = Video(
        id = id,
        title = title,
        description = "",
        publishedAt = Instant.parse(publishedAt),
        category = "tech",
        thumbnailUrl = "",
        location = null,
    )

    private val videos = listOf(
        video("1", "Middle Clip", "2026-03-01T10:00:00Z"),
        video("2", "ZZZ Newest Clip", "2026-06-20T10:00:00Z"),
        video("3", "AAA Oldest Clip", "2025-12-01T10:00:00Z"),
    )

    @Test
    fun `date newest sorts descending by publishedAt`() {
        val result = useCase(videos, SortOption.DATE_NEWEST)
        assertThat(result.map { it.id }).containsExactly("2", "1", "3").inOrder()
    }

    @Test
    fun `date oldest sorts ascending by publishedAt`() {
        val result = useCase(videos, SortOption.DATE_OLDEST)
        assertThat(result.map { it.id }).containsExactly("3", "1", "2").inOrder()
    }

    @Test
    fun `title a to z sorts alphabetically`() {
        val result = useCase(videos, SortOption.TITLE_A_TO_Z)
        assertThat(result.map { it.title }).containsExactly("AAA Oldest Clip", "Middle Clip", "ZZZ Newest Clip").inOrder()
    }

    @Test
    fun `title z to a sorts reverse alphabetically`() {
        val result = useCase(videos, SortOption.TITLE_Z_TO_A)
        assertThat(result.map { it.title }).containsExactly("ZZZ Newest Clip", "Middle Clip", "AAA Oldest Clip").inOrder()
    }

    @Test
    fun `empty list stays empty`() {
        assertThat(useCase(emptyList(), SortOption.DATE_NEWEST)).isEmpty()
    }
}
