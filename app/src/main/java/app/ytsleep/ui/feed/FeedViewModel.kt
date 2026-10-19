package app.ytsleep.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ytsleep.YtSleepApp
import app.ytsleep.youtube.ContentError
import app.ytsleep.youtube.FeedKind
import app.ytsleep.youtube.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedTabState(
    val items: List<MediaItem> = emptyList(),
    val nextToken: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: ContentError? = null,
    val loadMoreError: ContentError? = null,
)

class FeedViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as YtSleepApp).container
    private val contentSource = container.contentSource

    private val _homeState = MutableStateFlow(FeedTabState(isLoading = true))
    val homeState = _homeState.asStateFlow()

    private val _subsState = MutableStateFlow(FeedTabState(isLoading = true))
    val subsState = _subsState.asStateFlow()

    private val requested = mutableSetOf<FeedKind>()

    /** Loads a tab the first time it is shown. */
    fun ensureLoaded(kind: FeedKind) {
        if (requested.add(kind)) load(kind)
    }

    fun retry(kind: FeedKind) {
        stateFor(kind).value = FeedTabState(isLoading = true)
        load(kind)
    }

    fun play(item: MediaItem) = container.playerConnection.playItems(listOf(item), 0, null)

    private fun stateFor(kind: FeedKind) = if (kind == FeedKind.HOME) _homeState else _subsState

    private fun load(kind: FeedKind) {
        viewModelScope.launch {
            val state = stateFor(kind)
            try {
                val page = contentSource.feed(kind)
                state.value = FeedTabState(items = page.items, nextToken = page.nextToken)
            } catch (e: ContentError) {
                state.value = FeedTabState(error = e)
            }
        }
    }

    fun loadMore(kind: FeedKind) {
        val state = stateFor(kind)
        if (state.value.isLoadingMore) return
        val token = state.value.nextToken ?: return
        viewModelScope.launch {
            state.value = state.value.copy(isLoadingMore = true, loadMoreError = null)
            try {
                val page = contentSource.feed(kind, token)
                state.value = state.value.copy(
                    isLoadingMore = false,
                    items = state.value.items + page.items.filter { new -> state.value.items.none { it.videoId == new.videoId } },
                    nextToken = page.nextToken,
                )
            } catch (e: ContentError) {
                state.value = state.value.copy(isLoadingMore = false, loadMoreError = e)
            }
        }
    }
}
