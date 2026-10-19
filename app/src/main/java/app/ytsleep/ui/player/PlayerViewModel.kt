package app.ytsleep.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import app.ytsleep.YtSleepApp
import app.ytsleep.playback.PlayerConnection
import app.ytsleep.playback.PlayerUiState
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(app: Application) : AndroidViewModel(app) {
    val playerConnection: PlayerConnection = (app as YtSleepApp).container.playerConnection
    val state: StateFlow<PlayerUiState> = playerConnection.state

    fun playPause() = playerConnection.playPause()
    fun seekBack() = playerConnection.seekBack()
    fun seekForward() = playerConnection.seekForward()
    fun next() = playerConnection.next()
    fun previous() = playerConnection.previous()
    fun seekTo(ms: Long) = playerConnection.seekTo(ms)
    fun cycleRepeat() = playerConnection.cycleRepeatMode()
    fun setSleepTimer(minutes: Int) = playerConnection.setSleepTimer(minutes)
    fun setSleepTimerEndOfItem() = playerConnection.setSleepTimerEndOfItem()
    fun cancelSleepTimer() = playerConnection.cancelSleepTimer()
}
