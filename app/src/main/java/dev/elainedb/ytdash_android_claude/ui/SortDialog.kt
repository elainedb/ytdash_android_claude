package dev.elainedb.ytdash_android_claude.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.elainedb.ytdash_android_claude.viewmodel.SortOption

@Composable
fun SortDialog(
    currentSort: SortOption,
    onApply: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSort by remember { mutableStateOf(currentSort) }

    val sortOptions = listOf(
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
                sortOptions.forEach { (option, label) ->
                    RadioOption(
                        text = label,
                        selected = selectedSort == option,
                        onClick = { selectedSort = option }
                    )
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
