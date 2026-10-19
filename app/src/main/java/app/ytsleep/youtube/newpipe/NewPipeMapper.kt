package app.ytsleep.youtube.newpipe

import app.ytsleep.youtube.ContentFilter
import app.ytsleep.youtube.MediaItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

object NewPipeMapper {

    fun toMediaItem(item: StreamInfoItem): MediaItem {
        val url = item.url ?: ""
        val videoId = extractVideoId(url) ?: item.url?.takeLast(11) ?: ""

        val durationSeconds = item.duration.takeIf { it > 0 }?.toInt()

        val isMarkedShort = item.isShortFormContent || ContentFilter.isShortsUrl(url)

        val isLive = item.streamType == StreamType.LIVE_STREAM ||
                item.streamType == StreamType.AUDIO_LIVE_STREAM

        val thumbnail = item.thumbnails
            .filter { it.width <= 480 || it.width == Int.MIN_VALUE }
            .maxByOrNull { it.width }
            ?.url ?: item.thumbnails.firstOrNull()?.url ?: ""

        return MediaItem(
            videoId = videoId,
            title = item.name ?: "",
            channelName = item.uploaderName ?: "",
            channelId = item.uploaderUrl,
            thumbnailUrl = thumbnail,
            durationSeconds = durationSeconds,
            isMarkedShort = isMarkedShort,
            isLive = isLive,
        )
    }

    private fun extractVideoId(url: String): String? {
        return Regex("[?&]v=([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
            ?: Regex("/(?:watch\\?v=|embed/|v/|shorts/)([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
    }
}
