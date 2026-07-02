package com.example.ytdash.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private const val ALL_LABEL = "All categories"

/**
 * Replaces `video_list` while open (cross-framework-setup.md §D.2): avoids `text:` selector
 * collisions between an option label and a matching item title in the black-box driver.
 */
@Composable
fun FilterPanel(
    categories: List<String>,
    current: String?,
    onApply: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(current) }

    Column(modifier = modifier.fillMaxSize().testTag("filter_panel").padding(16.dp)) {
        Text("Filter by category", style = MaterialTheme.typography.titleMedium)

        OptionRow(text = ALL_LABEL, selected = selected == null) { selected = null }
        categories.forEach { category ->
            OptionRow(text = category, selected = selected == category) { selected = category }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onDismiss) { Text("Cancel") }
            Button(
                onClick = { onApply(selected) },
                modifier = Modifier.padding(start = 8.dp).testTag("filter_apply_button"),
            ) { Text("Apply") }
        }
    }
}

@Composable
private fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        // Rendered as the label's exact, unadorned text so Maestro's full-string `text:` regex
        // match (e.g. "(?i)${FILTER_LABEL}") resolves unambiguously.
        Text(text = text)
    }
}
