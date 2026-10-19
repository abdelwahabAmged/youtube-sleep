package app.ytsleep.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ytsleep.youtube.MediaItem

@Composable
fun MediaItemRow(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSaveButton: Boolean = true,
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    if (showSaveDialog) AddToPlaylistDialog(item, onDismiss = { showSaveDialog = false })

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = if (showSaveButton) 0.dp else 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Thumbnail(
            url = item.thumbnailUrl,
            badge = if (item.isLive) "LIVE" else item.durationSeconds?.let(::formatDuration),
            modifier = Modifier.width(168.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.channelName,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showSaveButton) {
            IconButton(onClick = { showSaveDialog = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Save to playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
