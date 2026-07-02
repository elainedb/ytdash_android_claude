package com.example.ytdash.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ytdash.domain.SortSpec

// Labels must END with the regex keyword the flows match — no trailing punctuation after it
// (cross-framework-setup.md §D.3: Maestro `text:` is a full-string regex match).
private val SORT_OPTIONS = listOf(
    SortSpec.DateNewestFirst to "Date - Newest",
    SortSpec.DateOldestFirst to "Date - Oldest",
    SortSpec.TitleAToZ to "Title A to Z",
)

/** Replaces the list while open, same rationale as [FilterSheet]. */
@Composable
fun SortSheet(
    current: SortSpec,
    onApply: (SortSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    var pending by remember(current) { mutableStateOf(current) }

    Column(Modifier.fillMaxSize().testTag("sort_sheet").padding(24.dp)) {
        Text("Sort videos", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        SORT_OPTIONS.forEach { (spec, label) ->
            Text(
                text = label,
                fontWeight = if (pending == spec) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth().clickable { pending = spec }.padding(12.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
                modifier = Modifier.testTag("sort_apply_button"),
                onClick = { onApply(pending) },
            ) { Text("Apply") }
        }
    }
}
