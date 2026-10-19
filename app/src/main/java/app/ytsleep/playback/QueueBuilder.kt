package app.ytsleep.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.MediaMetadata
import app.ytsleep.data.db.HistoryEntry
import app.ytsleep.youtube.MediaItem

object QueueBuilder {

    const val EXTRA_SOURCE_PLAYLIST_ID = "sourcePlaylistId"
    const val EXTRA_SOURCE_INDEX = "sourceIndex"
    const val EXTRA_DURATION_SECONDS = "durationSeconds"
    private const val SCHEME = "ytsleep"

    fun buildQueue(items: List<MediaItem>, sourcePlaylistId: String?): List<Media3Item> =
        items.mapIndexed { index, item ->
            build(
                videoId = item.videoId,
                title = item.title,
                channelName = item.channelName,
                thumbnailUrl = item.thumbnailUrl,
                durationSeconds = item.durationSeconds,
                sourcePlaylistId = sourcePlaylistId,
                sourceIndex = index,
            )
        }

    fun fromHistory(entry: HistoryEntry): Media3Item = build(
        videoId = entry.videoId,
        title = entry.title,
        channelName = entry.channelName,
        thumbnailUrl = entry.thumbnailUrl,
        durationSeconds = entry.durationSeconds,
        sourcePlaylistId = entry.sourcePlaylistId,
        sourceIndex = entry.sourceIndex,
    )

    private fun build(
        videoId: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        durationSeconds: Int?,
        sourcePlaylistId: String?,
        sourceIndex: Int?,
    ): Media3Item {
        val extras = Bundle().apply {
            sourcePlaylistId?.let { putString(EXTRA_SOURCE_PLAYLIST_ID, it) }
            sourceIndex?.let { putInt(EXTRA_SOURCE_INDEX, it) }
            durationSeconds?.let { putInt(EXTRA_DURATION_SECONDS, it) }
        }
        return Media3Item.Builder()
            .setUri(Uri.Builder().scheme(SCHEME).authority(videoId).build())
            .setMediaId(videoId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(channelName)
                    .setArtworkUri(thumbnailUrl.takeIf { it.isNotEmpty() }?.let(Uri::parse))
                    .setExtras(extras)
                    .build()
            )
            .build()
    }
}
