package com.example.ytdash.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Small alias so every screen applies the selector-contract id the same way. */
fun Modifier.testTagAs(tag: String): Modifier = this.testTag(tag)
