package app.ytsleep.playback

import androidx.annotation.OptIn
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import app.ytsleep.MainActivity
import app.ytsleep.R
import app.ytsleep.YtSleepApp
import app.ytsleep.data.RepeatMode
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    companion object {
        const val CMD_SET_SLEEP_TIMER = "app.ytsleep.SET_SLEEP_TIMER"
        const val CMD_CANCEL_SLEEP_TIMER = "app.ytsleep.CANCEL_SLEEP_TIMER"
        const val ARG_SLEEP_MINUTES = "minutes"
        const val ARG_END_OF_ITEM = "endOfItem"

        const val EXTRA_TIMER_ACTIVE = "sleepTimerActive"
        const val EXTRA_TIMER_KIND = "sleepTimerKind"
        const val EXTRA_TIMER_REMAINING_MS = "sleepTimerRemainingMs"
        const val EXTRA_PLAYER_ERROR = "playerError"
    }

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val errorPolicy = PlaybackErrorPolicy()
    private var sleepTimer: SleepTimer? = null
    private var timerTicker: Job? = null
    private var playerError: String? = null

    private val fadeTarget = object : FadeTarget {
        override fun setVolume(volume: Float) { player.volume = volume }
        override fun pause() { player.pause() }
    }

    override fun onCreate() {
        super.onCreate()
        val container = (applicationContext as YtSleepApp).container

        // Audio-only: the player has no video renderer, so no frame can ever be decoded or shown.
        val audioOnlyRenderers = RenderersFactory { handler, _, audioListener, _, _ ->
            arrayOf(MediaCodecAudioRenderer(this, MediaCodecSelector.DEFAULT, handler, audioListener))
        }

        // DefaultDataSource handles file/asset URIs itself and hands ytsleep:// to our resolver.
        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            StreamResolvingDataSource.Factory(container.contentSource),
        )

        player = ExoPlayer.Builder(this, audioOnlyRenderers)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus= */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(15_000)
            .setSeekForwardIncrementMs(30_000)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(dataSourceFactory)
                    .setLoadErrorHandlingPolicy(PatientLoadErrorPolicy())
            )
            .build()

        serviceScope.launch {
            player.repeatMode = when (container.settingsStore.repeatMode.first()) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                RepeatMode.PLAYLIST -> Player.REPEAT_MODE_ALL
            }
        }

        player.addListener(HistoryRecorder(player, container.database.historyDao(), serviceScope))
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val failure = PlaybackFailure.classify(error)
                if (!errorPolicy.onItemError(player, failure)) {
                    playerError = failure.message
                    publishExtras()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && playerError != null) {
                    playerError = null
                    publishExtras()
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.PLAYLIST
                    else -> RepeatMode.OFF
                }
                serviceScope.launch { container.settingsStore.setRepeatMode(mode) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val itemEnded = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
                if (itemEnded && sleepTimer?.kind == SleepTimerKind.END_OF_ITEM) finishEndOfItemTimer()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && sleepTimer?.kind == SleepTimerKind.END_OF_ITEM) {
                    finishEndOfItemTimer()
                }
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        // Seek −15 s / +30 s buttons for the notification and lock screen (FR-004).
        val seekButtons = ImmutableList.of(
            CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                .setIconResId(R.drawable.ic_replay_15)
                .setDisplayName("Back 15 seconds")
                .build(),
            CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                .setIconResId(R.drawable.ic_forward_30)
                .setDisplayName("Forward 30 seconds")
                .build(),
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCustomLayout(seekButtons)
            .setCallback(SessionCallback())
            .build()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CMD_SET_SLEEP_TIMER, Bundle.EMPTY))
                .add(SessionCommand(CMD_CANCEL_SLEEP_TIMER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_SET_SLEEP_TIMER ->
                    if (args.getBoolean(ARG_END_OF_ITEM, false)) startEndOfItemTimer()
                    else startDurationTimer(args.getInt(ARG_SLEEP_MINUTES, 30))
                CMD_CANCEL_SLEEP_TIMER -> cancelSleepTimer()
                else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun startDurationTimer(minutes: Int) {
        cancelSleepTimer()
        val container = (applicationContext as YtSleepApp).container
        serviceScope.launch { container.settingsStore.setLastSleepTimerMinutes(minutes) }
        val timer = SleepTimer(
            kind = SleepTimerKind.DURATION,
            endsAtElapsedMs = SystemClock.elapsedRealtime() + minutes * 60_000L,
        )
        sleepTimer = timer
        timer.run(fadeTarget, serviceScope, onFinished = ::clearSleepTimer)
        startTicker()
    }

    private fun startEndOfItemTimer() {
        cancelSleepTimer()
        sleepTimer = SleepTimer(SleepTimerKind.END_OF_ITEM, endsAtElapsedMs = null)
        startTicker()
    }

    /** Publishes the timer state every second and, for END_OF_ITEM, fades over the item's last 10 s. */
    private fun startTicker() {
        timerTicker?.cancel()
        timerTicker = serviceScope.launch {
            while (isActive) {
                val timer = sleepTimer ?: break
                if (timer.kind == SleepTimerKind.END_OF_ITEM && player.duration != C.TIME_UNSET &&
                    player.repeatMode != Player.REPEAT_MODE_ONE
                ) {
                    player.volume = SleepTimer.fadeVolume(player.duration - player.currentPosition, timer.fadeMs)
                }
                publishExtras()
                delay(1_000)
            }
            publishExtras()
        }
    }

    private fun finishEndOfItemTimer() {
        player.pause()
        player.seekTo(0)
        clearSleepTimer()
    }

    private fun cancelSleepTimer() {
        clearSleepTimer()
    }

    private fun clearSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        timerTicker?.cancel()
        timerTicker = null
        player.volume = 1f
        publishExtras()
    }

    private fun publishExtras() {
        val timer = sleepTimer
        mediaSession.setSessionExtras(Bundle().apply {
            putString(EXTRA_PLAYER_ERROR, playerError)
            putBoolean(EXTRA_TIMER_ACTIVE, timer != null)
            putString(EXTRA_TIMER_KIND, timer?.kind?.name.orEmpty())
            putLong(EXTRA_TIMER_REMAINING_MS, timer?.remainingMs?.value ?: 0L)
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        sleepTimer?.cancel()
        serviceScope.cancel()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
