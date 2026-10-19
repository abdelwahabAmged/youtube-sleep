package app.ytsleep.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import app.ytsleep.ui.common.formatDuration
import coil.compose.AsyncImage

@Composable
fun PlayerSheetHost(modifier: Modifier = Modifier, vm: PlayerViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val item = state.currentItem ?: return

    // While the user drags, show the drag position instead of the live one; seek on release.
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    Surface(modifier = modifier, tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Static thumbnail only — the player has no video surface (FR-001, FR-005).
                AsyncImage(
                    model = item.mediaMetadata.artworkUri,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaMetadata.title?.toString().orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    Text(
                        text = item.mediaMetadata.artist?.toString().orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                    state.error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                }
                IconButton(onClick = vm::playPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                    )
                }
            }

            if (state.durationMs > 0) {
                Slider(
                    value = dragPosition ?: state.positionMs.toFloat().coerceAtMost(state.durationMs.toFloat()),
                    onValueChange = { dragPosition = it },
                    onValueChangeFinished = {
                        dragPosition?.let { vm.seekTo(it.toLong()) }
                        dragPosition = null
                    },
                    valueRange = 0f..state.durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val shownMs = dragPosition?.toLong() ?: state.positionMs
                    Text(formatDuration((shownMs / 1000).toInt()), style = MaterialTheme.typography.labelSmall)
                    Text(formatDuration((state.durationMs / 1000).toInt()), style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = vm::previous) { Icon(Icons.Default.SkipPrevious, "Previous") }
                IconButton(onClick = vm::seekBack) { Icon(Icons.Default.Replay, "Back 15 seconds") }
                IconButton(onClick = vm::seekForward) { Icon(Icons.Default.Forward30, "Forward 30 seconds") }
                IconButton(onClick = vm::next) { Icon(Icons.Default.SkipNext, "Next") }
                RepeatButton(repeatMode = state.repeatMode, onClick = vm::cycleRepeat)
                SleepTimerButton(
                    active = state.sleepTimerActive,
                    endOfItem = state.sleepTimerEndOfItem,
                    remainingMs = state.sleepTimerRemainingMs,
                    onSetTimer = vm::setSleepTimer,
                    onSetEndOfItem = vm::setSleepTimerEndOfItem,
                    onCancel = vm::cancelSleepTimer,
                )
            }
        }
    }
}

@Composable
private fun RepeatButton(repeatMode: Int, onClick: () -> Unit) {
    val (icon, label) = when (repeatMode) {
        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne to "Repeat one"
        Player.REPEAT_MODE_ALL -> Icons.Default.Repeat to "Repeat playlist"
        else -> Icons.Default.Repeat to "Repeat off"
    }
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                LocalContentColor.current.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
