package com.example.ytdash.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---- search.list / playlistItems.list ----

@Serializable
data class SearchListResponse(
    val items: List<SearchItem> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class SearchItem(
    val id: SearchId? = null,
    val snippet: Snippet? = null,
)

@Serializable
data class SearchId(
    val videoId: String? = null,
)

@Serializable
data class Snippet(
    val title: String = "",
    val description: String = "",
    val publishedAt: String = "",
    val channelTitle: String = "",
    val thumbnails: Thumbnails? = null,
    val resourceId: ResourceId? = null, // present in playlistItems
)

@Serializable
data class ResourceId(
    val videoId: String? = null,
)

@Serializable
data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
)

@Serializable
data class Thumbnail(
    val url: String = "",
)

// ---- playlistItems.list ----
// Real YouTube returns items[].id as a STRING (the playlist-item id); we only need
// snippet.resourceId.videoId, so `id` is intentionally NOT declared (ignored via ignoreUnknownKeys).

@Serializable
data class PlaylistItemsResponse(
    val items: List<PlaylistItemDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class PlaylistItemDto(
    val snippet: Snippet? = null,
)

// ---- channels.list ----

@Serializable
data class ChannelListResponse(
    val items: List<ChannelItem> = emptyList(),
)

@Serializable
data class ChannelItem(
    val id: String = "",
    val contentDetails: ChannelContentDetails? = null,
)

@Serializable
data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists? = null,
)

@Serializable
data class RelatedPlaylists(
    val uploads: String? = null,
)

// ---- videos.list ----

@Serializable
data class VideoListResponse(
    val items: List<VideoItem> = emptyList(),
)

@Serializable
data class VideoItem(
    val id: String = "",
    val snippet: Snippet? = null,
    val recordingDetails: RecordingDetails? = null,
)

@Serializable
data class RecordingDetails(
    val location: Location? = null,
)

@Serializable
data class Location(
    val latitude: Double? = null,
    val longitude: Double? = null,
)
