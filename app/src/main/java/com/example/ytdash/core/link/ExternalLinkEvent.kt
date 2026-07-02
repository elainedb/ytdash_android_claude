package com.example.ytdash.core.link

sealed interface ExternalLinkEvent {
    data class Captured(val url: String) : ExternalLinkEvent
    data object Error : ExternalLinkEvent
}
