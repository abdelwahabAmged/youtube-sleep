package app.ytsleep.playback

import androidx.media3.common.Player

/** Skips an unplayable item to the next one in the queue; each item is skipped at most once. */
class PlaybackErrorPolicy {

    private val skipped = mutableSetOf<String>()

    /** Returns true if playback moved on to the next item. */
    fun onItemError(player: Player, failure: PlaybackFailure): Boolean {
        if (failure !is PlaybackFailure.Unplayable) return false
        val videoId = player.currentMediaItem?.mediaId ?: return false
        if (!skipped.add(videoId)) return false
        if (!player.hasNextMediaItem()) return false
        player.seekToNextMediaItem()
        player.prepare()
        player.play()
        return true
    }
}
