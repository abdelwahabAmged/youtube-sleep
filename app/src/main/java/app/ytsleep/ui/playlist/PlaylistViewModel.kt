package app.ytsleep.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import app.ytsleep.YtSleepApp
import app.ytsleep.youtube.ContentError
import app.ytsleep.youtube.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val items: List<MediaItem> = emptyList(),
    val nextToken: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: ContentError? = null,
    val loadMoreError: ContentError? = null,
)

class PlaylistViewModel(app: Application, private val playlistId: String) : AndroidViewModel(app) {

    private val container = (app as YtSleepApp).container
    private val _state = MutableStateFlow(PlaylistUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = PlaylistUiState(isLoading = true)
        viewModelScope.launch {
            _state.value = try {
                val page = container.contentSource.playlistItems(playlistId)
                PlaylistUiState(items = page.items, nextToken = page.nextToken, isLoading = false)
            } catch (e: ContentError) {
                PlaylistUiState(isLoading = false, error = e)
            }
        }
    }

    fun loadMore() {
        if (_state.value.isLoadingMore) return
        val token = _state.value.nextToken ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true, loadMoreError = null)
            _state.value = try {
                val page = container.contentSource.playlistItems(playlistId, token)
                _state.value.copy(
                    isLoadingMore = false,
                    items = _state.value.items + page.items,
                    nextToken = page.nextToken,
                )
            } catch (e: ContentError) {
                _state.value.copy(isLoadingMore = false, loadMoreError = e)
            }
        }
    }

    fun playFrom(index: Int) {
        val items = _state.value.items
        if (items.isNotEmpty()) container.playerConnection.playItems(items, index, playlistId)
    }
}

class PlaylistViewModelFactory(private val playlistId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        @Suppress("UNCHECKED_CAST")
        return PlaylistViewModel(app, playlistId) as T
    }
}
