package app.ytsleep.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.ytsleep.youtube.FeedKind

@Composable
fun FeedScreen(navController: NavController, vm: FeedViewModel = viewModel()) {
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val subsState by vm.subsState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Subscriptions")
    val selectedKind = if (selectedTab == 0) FeedKind.HOME else FeedKind.SUBSCRIPTIONS
    val homeListState = rememberLazyListState()
    val subsListState = rememberLazyListState()
    LaunchedEffect(selectedKind) { vm.ensureLoaded(selectedKind) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
            }
        }
        val state = if (selectedTab == 0) homeState else subsState
        val listState = if (selectedTab == 0) homeListState else subsListState
        LoadMoreOnScroll(listState, state.nextToken, state.isLoadingMore, state.loadMoreError) {
            vm.loadMore(selectedKind)
        }
        when {
            state.isLoading -> LoadingPlaceholder()
            state.error != null -> {
                val (label, action) = accountAction(state.error, navController)
                    ?: ("Retry" to { vm.retry(selectedKind) })
                StatusMessage(error = state.error, onAction = action, actionLabel = label)
            }
            else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                itemsIndexed(state.items, key = { index, item -> "$index:${item.videoId}" }) { _, item ->
                    MediaItemRow(item = item, onClick = { vm.play(item) })
                }
                item {
                    LoadMoreFooter(
                        nextToken = state.nextToken,
                        error = state.loadMoreError,
                        onRetry = { vm.loadMore(selectedKind) },
                    )
                }
            }
        }
    }
}
