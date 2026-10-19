package app.ytsleep.ui.start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.ytsleep.data.db.HistoryEntry
import app.ytsleep.ui.common.PlaylistRow
import app.ytsleep.ui.playlistRoute
import app.ytsleep.ui.common.formatDuration
import app.ytsleep.update.UpdateState
import coil.compose.AsyncImage

@Composable
fun StartScreen(
    navController: NavController,
    vm: StartViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    BatteryOptimizationPrompt()

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        item { UpdateBanner(state.updateState) }

        state.continueListening?.let { entry ->
            item { SectionTitle("Continue listening") }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { vm.resume(entry) },
                ) {
                    HistoryRow(entry, showPosition = true, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (state.recent.isNotEmpty()) {
            item { SectionTitle("Recent") }
            items(state.recent, key = { it.videoId }) { entry ->
                HistoryRow(
                    entry,
                    showPosition = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.resume(entry) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.playlists.isNotEmpty()) {
            item { SectionTitle("My playlists") }
            items(state.playlists, key = { it.playlistId }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    onClick = { navController.navigate(playlistRoute(playlist)) },
                )
            }
        }

        if (state.continueListening == null && state.recent.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(32.dp)) {
                    Text(
                        "Nothing played yet. Search for something to listen to.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("search") }) { Text("Search") }
                    if (state.playlists.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { navController.navigate("account/connect") }) {
                            Text("Connect your YouTube account")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun HistoryRow(entry: HistoryEntry, showPosition: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = entry.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.width(96.dp).height(54.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text(entry.channelName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            if (showPosition && entry.lastPositionMs > 0) {
                val position = formatDuration((entry.lastPositionMs / 1000).toInt())
                val total = entry.durationSeconds?.let { " / ${formatDuration(it)}" }.orEmpty()
                Text("$position$total", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
