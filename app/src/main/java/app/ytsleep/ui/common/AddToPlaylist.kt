package app.ytsleep.ui.common

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.ytsleep.YtSleepApp
import app.ytsleep.youtube.ContentError
import app.ytsleep.youtube.MediaItem
import app.ytsleep.youtube.PlaylistSummary
import kotlinx.coroutines.launch

/** "Liked videos" is filled by liking, not by adding, so it isn't offered here. */
private fun List<PlaylistSummary>.saveTargets() = filter { it.playlistId != "LL" }

/** Lets the user save [video] to Watch later or one of their playlists on their YouTube account. */
@Composable
fun AddToPlaylistDialog(video: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as YtSleepApp).container
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<PlaylistSummary>?>(null) }
    var error by remember { mutableStateOf<ContentError?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        container.playlistCache.read().takeIf { it.isNotEmpty() }?.let { playlists = it.saveTargets() }
        try {
            val fresh = container.contentSource.myPlaylists()
            container.playlistCache.write(fresh)
            playlists = fresh.saveTargets()
        } catch (e: ContentError) {
            if (playlists == null) error = e
        }
    }

    fun save(playlist: PlaylistSummary) {
        saving = true
        scope.launch {
            val message = try {
                container.contentSource.addToPlaylist(playlist.playlistId, video.videoId)
                "Saved to ${playlist.title}"
            } catch (e: ContentError) {
                when (e) {
                    is ContentError.Offline, is ContentError.NotSignedIn, is ContentError.SessionExpired -> errorMessage(e)
                    else -> "Couldn't save to ${playlist.title}. You can only add to your own playlists."
                }
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to…") },
        text = {
            val current = playlists
            when {
                error != null -> Text(errorMessage(error!!))
                current == null -> Text("Loading your playlists…")
                current.isEmpty() -> Text("You don't have any playlists yet.")
                else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(current, key = { it.playlistId }) { playlist ->
                        Text(
                            text = playlist.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !saving) { save(playlist) }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
