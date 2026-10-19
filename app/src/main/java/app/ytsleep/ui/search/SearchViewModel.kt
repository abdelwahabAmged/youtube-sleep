package app.ytsleep.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ytsleep.YtSleepApp
import app.ytsleep.youtube.ContentError
import app.ytsleep.youtube.MediaItem
import app.ytsleep.youtube.Page
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val items: List<MediaItem> = emptyList(),
    val nextToken: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: ContentError? = null,
    val loadMoreError: ContentError? = null,
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as YtSleepApp).container
    private val contentSource = container.contentSource

    fun play(item: MediaItem) = container.playerConnection.playItems(listOf(item), 0, null)
    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, items = emptyList(), nextToken = null)
            try {
                val page = contentSource.search(q)
                _state.value = _state.value.copy(
                    isLoading = false,
                    items = page.items,
                    nextToken = page.nextToken,
                )
            } catch (e: ContentError) {
                _state.value = _state.value.copy(isLoading = false, error = e)
            }
        }
    }

    fun loadMore() {
        if (_state.value.isLoadingMore) return
        val token = _state.value.nextToken ?: return
        val q = _state.value.query
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true, loadMoreError = null)
            try {
                val page = contentSource.search(q, token)
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    items = _state.value.items + page.items.filter { new -> _state.value.items.none { it.videoId == new.videoId } },
                    nextToken = page.nextToken,
                )
            } catch (e: ContentError) {
                _state.value = _state.value.copy(isLoadingMore = false, loadMoreError = e)
            }
        }
    }
}
