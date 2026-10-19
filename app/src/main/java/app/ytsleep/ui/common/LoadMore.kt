package app.ytsleep.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.ytsleep.youtube.ContentError
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Infinite scrolling: asks for the next page once the user scrolls within [prefetchItems] of the
 * end. Stops after a failed page so a broken connection doesn't retry in a loop.
 */
@Composable
fun LoadMoreOnScroll(
    listState: LazyListState,
    nextToken: String?,
    isLoading: Boolean,
    error: ContentError?,
    prefetchItems: Int = 5,
    onLoadMore: () -> Unit,
) {
    val canLoad by rememberUpdatedState(nextToken != null && !isLoading && error == null)
    val load by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            canLoad && lastVisible >= info.totalItemsCount - 1 - prefetchItems
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { load() }
    }
}

/** Last row of a paged list: a quiet "Loading more…" while pages load, or the error with "Try again". */
@Composable
fun LoadMoreFooter(
    nextToken: String?,
    error: ContentError?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nextToken == null) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error == null) {
            Text("Loading more…", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(errorMessage(error), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRetry) { Text("Try again") }
        }
    }
}
