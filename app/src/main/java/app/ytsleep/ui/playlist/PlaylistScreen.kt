package app.ytsleep.ui.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.ytsleep.ui.common.LoadMoreFooter
import app.ytsleep.ui.common.LoadMoreOnScroll
import app.ytsleep.ui.common.LoadingPlaceholder
import app.ytsleep.ui.common.MediaItemRow
import app.ytsleep.ui.common.StatusMessage
import app.ytsleep.ui.common.accountAction

@Composable
fun PlaylistScreen(
    playlistId: String,
    title: String,
    itemCount: Int?,
    navController: NavController,
    vm: PlaylistViewModel = viewModel(key = playlistId, factory = PlaylistViewModelFactory(playlistId)),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LoadMoreOnScroll(listState, state.nextToken, state.isLoadingMore, state.loadMoreError, onLoadMore = vm::loadMore)

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
            itemCount?.let {
                Text("$it items", style = MaterialTheme.typography.bodySmall)
            }
        }

        val error = state.error
        when {
            state.isLoading -> LoadingPlaceholder()
            error != null -> {
                val (label, action) = accountAction(error, navController) ?: ("Retry" to vm::load)
                StatusMessage(error = error, onAction = action, actionLabel = label, modifier = Modifier.fillMaxSize())
            }
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                item {
                    Button(
                        onClick = { vm.playFrom(0) },
                        enabled = state.items.isNotEmpty(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) { Text("Play all") }
                }
                itemsIndexed(state.items, key = { index, item -> "$index:${item.videoId}" }) { index, item ->
                    MediaItemRow(item = item, onClick = { vm.playFrom(index) })
                }
                item {
                    LoadMoreFooter(
                        nextToken = state.nextToken,
                        error = state.loadMoreError,
                        onRetry = vm::loadMore,
                    )
                }
            }
        }
    }
}
