package dev.elainedb.ytdash_android_claude.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.elainedb.ytdash_android_claude.viewmodel.SortOption

@Composable
fun SortDialog(
    currentSort: SortOption,
    onApply: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }

    val sortLabels = mapOf(
        SortOption.PUBLISHED_DESC to "Publication Date (Newest First)",
        SortOption.PUBLISHED_ASC to "Publication Date (Oldest First)",
        SortOption.RECORDING_DESC to "Recording Date (Newest First)",
        SortOption.RECORDING_ASC to "Recording Date (Oldest First)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Videos") },
        text = {
            Column {
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedSort == option,
                                onClick = { selectedSort = option },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedSort == option, onClick = null)
                        Text(
                            text = sortLabels[option] ?: option.name,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedSort) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
