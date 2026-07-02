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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

/**
 * Replaces the list while open (cross-framework-setup.md §D.2) so option text ("tech") can't
 * collide with in-list item titles ("Tech Talk One") for a black-box selector.
 */
@Composable
fun FilterSheet(
    categories: List<String>,
    current: String?,
    onApply: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var pending by remember(current) { mutableStateOf(current) }

    Column(Modifier.fillMaxSize().testTag("filter_sheet").padding(24.dp)) {
        Text("Filter by category", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f)) {
            item {
                Text(
                    text = "All",
                    fontWeight = if (pending == null) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth().clickable { pending = null }.padding(12.dp),
                )
            }
            items(categories) { category ->
                Text(
                    text = category,
                    fontWeight = if (pending == category) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth().clickable { pending = category }.padding(12.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.width(8.dp))
            Button(
                modifier = Modifier.testTag("filter_apply_button"),
                onClick = { onApply(pending) },
            ) { Text("Apply") }
        }
    }
}
