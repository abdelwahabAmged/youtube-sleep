package app.ytsleep.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import app.ytsleep.ui.common.LoadMoreFooter
import app.ytsleep.ui.common.LoadMoreOnScroll
import app.ytsleep.ui.common.LoadingPlaceholder
import app.ytsleep.ui.common.MediaItemRow
import app.ytsleep.ui.common.StatusMessage

@Composable
fun SearchScreen(
    navController: NavController,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    // Open ready to type, unless we're coming back to earlier results.
    LaunchedEffect(Unit) { if (state.query.isEmpty()) focusRequester.requestFocus() }
    LoadMoreOnScroll(listState, state.nextToken, state.isLoadingMore, state.loadMoreError, onLoadMore = vm::loadMore)
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChanged,
            placeholder = { Text("Search YouTube audio") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                vm.search()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester),
        )

        when {
            state.isLoading -> LoadingPlaceholder()
            state.error != null -> StatusMessage(
                error = state.error!!,
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                itemsIndexed(state.items, key = { index, item -> "$index:${item.videoId}" }) { _, item ->
                    MediaItemRow(item = item, onClick = { vm.play(item) })
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
