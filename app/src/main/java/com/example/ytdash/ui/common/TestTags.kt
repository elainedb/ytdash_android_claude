package com.example.ytdash.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Apply a stable logical id (constitution §3). With `testTagsAsResourceId = true` set on the root,
 * these surface to Maestro as resource-ids, identically to the Flutter/RN identifier mechanisms.
 */
fun Modifier.tag(id: String): Modifier = this.testTag(id)
