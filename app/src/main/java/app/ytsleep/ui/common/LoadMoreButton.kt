package app.ytsleep.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ytsleep.youtube.ContentError

/** Explicit paging (FR-013). A failed page shows its error here and keeps the loaded items above. */
@Composable
fun LoadMoreButton(
    nextToken: String?,
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    error: ContentError? = null,
) {
    if (nextToken == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        error?.let {
            Text(
                errorMessage(it),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onLoadMore, enabled = !isLoading) {
            Text(
                when {
                    isLoading -> "Loading…"
                    error != null -> "Try again"
                    else -> "Load more"
                }
            )
        }
    }
}
