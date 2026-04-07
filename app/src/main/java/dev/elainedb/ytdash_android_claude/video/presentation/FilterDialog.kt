package dev.elainedb.ytdash_android_claude.video.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
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

@Composable
fun FilterDialog(
    currentFilter: FilterOptions,
    availableChannels: List<String>,
    availableCountries: List<String>,
    onApply: (FilterOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedChannel by remember { mutableStateOf(currentFilter.channelName) }
    var selectedCountry by remember { mutableStateOf(currentFilter.country) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Videos") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Channel", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))

                RadioOption(
                    text = "All Channels",
                    selected = selectedChannel == null,
                    onClick = { selectedChannel = null }
                )
                availableChannels.forEach { channel ->
                    RadioOption(
                        text = channel,
                        selected = selectedChannel == channel,
                        onClick = { selectedChannel = channel }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Country", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))

                RadioOption(
                    text = "All Countries",
                    selected = selectedCountry == null,
                    onClick = { selectedCountry = null }
                )
                availableCountries.forEach { country ->
                    RadioOption(
                        text = country,
                        selected = selectedCountry == country,
                        onClick = { selectedCountry = country }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(FilterOptions(channelName = selectedChannel, country = selectedCountry))
            }) {
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

@Composable
private fun RadioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
