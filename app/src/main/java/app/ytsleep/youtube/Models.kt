package app.ytsleep.youtube

data class MediaItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String?,
    val thumbnailUrl: String,
    val durationSeconds: Int?,   // null for live or unknown
    val isMarkedShort: Boolean,
    val isLive: Boolean,
)

data class PlaylistSummary(
    val playlistId: String,      // includes special ids WL (Watch later) and LL (Liked videos)
    val title: String,
    val thumbnailUrl: String?,
    val itemCount: Int?,
)

data class Page<T>(
    val items: List<T>,
    val nextToken: String?,      // null means the end
)

enum class FeedKind { HOME, SUBSCRIPTIONS }

data class AudioStream(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val contentLength: Long?,
    val expiresAtEpochMs: Long?,
)
