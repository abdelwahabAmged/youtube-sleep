package app.ytsleep.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import app.ytsleep.data.db.HistoryDao
import app.ytsleep.data.db.HistoryEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object HistoryRules {
    const val MAX_HISTORY = 500
    private const val END_THRESHOLD_MS = 30_000L

    fun savePosition(positionMs: Long, durationSeconds: Int?): Long {
        if (durationSeconds == null) return positionMs
        val endThresholdMs = durationSeconds * 1000L - END_THRESHOLD_MS
        return if (positionMs >= endThresholdMs) 0L else positionMs
    }
}

/** Must be called on the player's (main) thread. */
class HistoryRecorder(
    private val player: Player,
    private val dao: HistoryDao,
    private val scope: CoroutineScope,
) : Player.Listener {

    private var saveJob: Job? = null
    private var pruned = false
    private var current: MediaItem? = null
    private var currentDurationMs: Long = C.TIME_UNSET
    private var lastPositionMs = 0L
    // Only items whose audio actually started are recorded, so unplayable ones never show up
    // under "Continue listening" or "Recent".
    private var currentPlayed = false

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            currentPlayed = true
            saveCurrent()
            startPeriodicSave()
        } else {
            saveJob?.cancel()
            saveCurrent()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val previous = current
        if (previous != null && currentPlayed) {
            val ended = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            val endPosition = if (ended && currentDurationMs != C.TIME_UNSET) currentDurationMs else lastPositionMs
            save(previous, endPosition, currentDurationMs)
        }
        current = mediaItem
        currentDurationMs = C.TIME_UNSET
        lastPositionMs = player.currentPosition
        currentPlayed = false
    }

    private fun startPeriodicSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            while (isActive) {
                delay(10_000)
                saveCurrent()
            }
        }
    }

    private fun saveCurrent() {
        if (!currentPlayed) return
        val item = player.currentMediaItem ?: return
        current = item
        lastPositionMs = player.currentPosition
        if (player.duration != C.TIME_UNSET) currentDurationMs = player.duration
        save(item, lastPositionMs, currentDurationMs)
    }

    private fun save(item: MediaItem, positionMs: Long, playerDurationMs: Long) {
        val meta = item.mediaMetadata
        val extras = meta.extras
        val durationSeconds = extras?.getInt(QueueBuilder.EXTRA_DURATION_SECONDS, -1)?.takeIf { it > 0 }
            ?: playerDurationMs.takeIf { it != C.TIME_UNSET && it > 0 }?.let { (it / 1000).toInt() }
        val entry = HistoryEntry(
            videoId = item.mediaId,
            title = meta.title?.toString().orEmpty(),
            channelName = meta.artist?.toString().orEmpty(),
            thumbnailUrl = meta.artworkUri?.toString().orEmpty(),
            durationSeconds = durationSeconds,
            lastPositionMs = HistoryRules.savePosition(positionMs, durationSeconds),
            lastPlayedAt = System.currentTimeMillis(),
            sourcePlaylistId = extras?.getString(QueueBuilder.EXTRA_SOURCE_PLAYLIST_ID),
            sourceIndex = extras?.takeIf { it.containsKey(QueueBuilder.EXTRA_SOURCE_INDEX) }
                ?.getInt(QueueBuilder.EXTRA_SOURCE_INDEX),
        )
        scope.launch {
            dao.upsert(entry)
            if (!pruned) {
                dao.pruneKeepingNewest(HistoryRules.MAX_HISTORY)
                pruned = true
            }
        }
    }
}
