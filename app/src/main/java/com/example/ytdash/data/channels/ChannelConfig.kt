package com.example.ytdash.data.channels

import kotlinx.serialization.Serializable

@Serializable
data class ChannelConfig(
    val id: String,
    val label: String,
)
