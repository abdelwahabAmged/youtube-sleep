package app.ytsleep.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.ytsleep.youtube.PlaylistSummary
import coil.compose.AsyncImage

@Composable
fun PlaylistRow(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(playlist.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            playlist.itemCount?.let {
                Text("$it items", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
