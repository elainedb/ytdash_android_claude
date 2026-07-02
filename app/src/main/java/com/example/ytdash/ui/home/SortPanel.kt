package com.example.ytdash.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ytdash.domain.usecase.SortOrder

// Labels deliberately END with the flow's regex keyword (cross-framework-setup.md §D.3): Maestro's
// `text:` selector is a FULL-STRING match, so "(?i)date.*(desc|newest)" only matches a label whose
// last characters literally are "desc"/"newest" — no trailing punctuation after the keyword.
private val SORT_OPTIONS = listOf(
    SortOrder.DATE_DESC to "Date - Newest",
    SortOrder.DATE_ASC to "Date - Oldest",
    SortOrder.TITLE_ASC to "Title A-Z",
)

@Composable
fun SortPanel(
    current: SortOrder,
    onApply: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(current) }

    Column(modifier = modifier.fillMaxSize().testTag("sort_panel").padding(16.dp)) {
        Text("Sort videos", style = MaterialTheme.typography.titleMedium)

        SORT_OPTIONS.forEach { (order, label) ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { selected = order }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected == order, onClick = { selected = order })
                Text(text = label)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onDismiss) { Text("Cancel") }
            Button(
                onClick = { onApply(selected) },
                modifier = Modifier.padding(start = 8.dp).testTag("sort_apply_button"),
            ) { Text("Apply") }
        }
    }
}
