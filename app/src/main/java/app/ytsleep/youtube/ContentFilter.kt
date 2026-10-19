package app.ytsleep.youtube

object ContentFilter {

    const val SHORTS_MAX_SECONDS = 180

    fun isShort(item: MediaItem): Boolean =
        item.isMarkedShort || (item.durationSeconds != null && item.durationSeconds <= SHORTS_MAX_SECONDS)

    fun filter(items: List<MediaItem>): List<MediaItem> = items.filterNot { isShort(it) }

    fun isShortsUrl(url: String): Boolean = url.contains("/shorts/")
}
