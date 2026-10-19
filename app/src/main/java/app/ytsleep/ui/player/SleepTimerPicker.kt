package app.ytsleep.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val PRESET_MINUTES = listOf(15, 30, 45, 60, 90)

@Composable
fun SleepTimerButton(
    active: Boolean,
    endOfItem: Boolean,
    remainingMs: Long,
    onSetTimer: (minutes: Int) -> Unit,
    onSetEndOfItem: () -> Unit,
    onCancel: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    IconButton(onClick = { showPicker = true }) {
        Icon(
            imageVector = Icons.Default.Bedtime,
            contentDescription = "Sleep timer",
            tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
    }

    if (showPicker) {
        SleepTimerPickerDialog(
            active = active,
            endOfItem = endOfItem,
            remainingMs = remainingMs,
            onSetTimer = { onSetTimer(it); showPicker = false },
            onSetEndOfItem = { onSetEndOfItem(); showPicker = false },
            onCancel = { onCancel(); showPicker = false },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepTimerPickerDialog(
    active: Boolean,
    endOfItem: Boolean,
    remainingMs: Long,
    onSetTimer: (minutes: Int) -> Unit,
    onSetEndOfItem: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    // A plain dialog rather than a bottom sheet: it appears without a slide animation (FR-014).
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (active) {
                    Text(
                        text = if (endOfItem) "Stops at the end of this item"
                        else "Stops in ${formatRemainingMs(remainingMs)}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onCancel) { Text("Cancel timer") }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESET_MINUTES.forEach { minutes ->
                        FilterChip(
                            selected = false,
                            onClick = { onSetTimer(minutes) },
                            label = { Text("$minutes min") },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onSetEndOfItem, modifier = Modifier.fillMaxWidth()) {
                    Text("End of current item")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private fun formatRemainingMs(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000).toInt()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
