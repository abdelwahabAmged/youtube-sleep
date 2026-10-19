package app.ytsleep.ui.start

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ytsleep.YtSleepApp
import app.ytsleep.data.db.HistoryEntry
import app.ytsleep.update.UpdateState
import app.ytsleep.youtube.PlaylistSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StartUiState(
    val continueListening: HistoryEntry? = null,
    val recent: List<HistoryEntry> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val updateState: UpdateState = UpdateState.NONE,
)

class StartViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as YtSleepApp).container
    private val dao = container.database.historyDao()
    private val playlists = MutableStateFlow<List<PlaylistSummary>>(emptyList())

    val state: StateFlow<StartUiState> = combine(
        dao.latest(),
        dao.recent(20),
        playlists,
        container.updateChecker.state,
    ) { latest, recent, lists, update ->
        StartUiState(
            continueListening = latest,
            // "Continue listening" already shows the latest entry.
            recent = recent.filter { it.videoId != latest?.videoId },
            playlists = lists,
            updateState = update,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StartUiState())

    init {
        viewModelScope.launch {
            try {
                playlists.value = container.contentSource.myPlaylists()
            } catch (_: Exception) {
                // Offline or not signed in: the playlists section stays hidden.
            }
        }
    }

    fun resume(entry: HistoryEntry) = container.playerConnection.resume(entry)
}
