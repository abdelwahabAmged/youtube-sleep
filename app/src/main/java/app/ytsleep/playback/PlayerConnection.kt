package app.ytsleep.playback

import androidx.annotation.OptIn
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import app.ytsleep.data.db.HistoryEntry
import app.ytsleep.youtube.MediaItem as YtMediaItem
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val connected: Boolean = false,
    val currentItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val sleepTimerActive: Boolean = false,
    val sleepTimerEndOfItem: Boolean = false,
    val sleepTimerRemainingMs: Long = 0L,
    val error: String? = null,
)

/** UI-side wrapper around a MediaController connected to [PlaybackService]. Main thread only. */
@OptIn(UnstableApi::class)
class PlayerConnection(context: Context) {

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val controllerFuture: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var positionTicker: Job? = null

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token)
            .setListener(object : MediaController.Listener {
                override fun onExtrasChanged(controller: MediaController, extras: Bundle) = updateState()
            })
            .buildAsync()
        controllerFuture.addListener({
            val c = controllerFuture.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) = updateState()
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) startPositionTicker() else positionTicker?.cancel()
                }
            })
            pending.forEach { it(c) }
            pending.clear()
            updateState()
        }, MoreExecutors.directExecutor())
    }

    /** Runs [action] now, or as soon as the controller connects, so early taps are not lost. */
    private fun withController(action: (MediaController) -> Unit) {
        controller?.let(action) ?: pending.add(action)
    }

    private fun startPositionTicker() {
        positionTicker?.cancel()
        positionTicker = scope.launch {
            while (isActive) {
                updateState()
                delay(1_000)
            }
        }
    }

    private fun updateState() {
        val c = controller ?: return
        val extras = c.sessionExtras
        _state.value = PlayerUiState(
            connected = true,
            currentItem = c.currentMediaItem,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = c.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L,
            repeatMode = c.repeatMode,
            sleepTimerActive = extras.getBoolean(PlaybackService.EXTRA_TIMER_ACTIVE, false),
            sleepTimerEndOfItem = extras.getString(PlaybackService.EXTRA_TIMER_KIND) == SleepTimerKind.END_OF_ITEM.name,
            sleepTimerRemainingMs = extras.getLong(PlaybackService.EXTRA_TIMER_REMAINING_MS, 0L),
            error = extras.getString(PlaybackService.EXTRA_PLAYER_ERROR),
        )
    }

    fun playItems(items: List<YtMediaItem>, startIndex: Int, sourcePlaylistId: String?) {
        if (items.isEmpty()) return
        val queue = QueueBuilder.buildQueue(items, sourcePlaylistId)
        withController { c ->
            c.setMediaItems(queue, startIndex.coerceIn(0, queue.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun resume(entry: HistoryEntry) {
        val item = QueueBuilder.fromHistory(entry)
        withController { c ->
            c.setMediaItem(item, entry.lastPositionMs)
            c.prepare()
            c.play()
        }
    }

    fun playPause() = withController {
        when {
            it.isPlaying -> it.pause()
            // After an error the player is idle and must be prepared again before it can play.
            it.playbackState == Player.STATE_IDLE -> { it.prepare(); it.play() }
            else -> it.play()
        }
    }
    fun seekBack() = withController { it.seekBack() }
    fun seekForward() = withController { it.seekForward() }
    fun next() = withController { it.seekToNextMediaItem() }
    fun previous() = withController { it.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms); updateState() }

    fun cycleRepeatMode() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSleepTimer(minutes: Int) = sendTimerCommand(
        PlaybackService.CMD_SET_SLEEP_TIMER,
        Bundle().apply { putInt(PlaybackService.ARG_SLEEP_MINUTES, minutes) },
    )

    fun setSleepTimerEndOfItem() = sendTimerCommand(
        PlaybackService.CMD_SET_SLEEP_TIMER,
        Bundle().apply { putBoolean(PlaybackService.ARG_END_OF_ITEM, true) },
    )

    fun cancelSleepTimer() = sendTimerCommand(PlaybackService.CMD_CANCEL_SLEEP_TIMER, Bundle.EMPTY)

    private fun sendTimerCommand(action: String, args: Bundle) = withController {
        it.sendCustomCommand(SessionCommand(action, Bundle.EMPTY), args)
    }
}
