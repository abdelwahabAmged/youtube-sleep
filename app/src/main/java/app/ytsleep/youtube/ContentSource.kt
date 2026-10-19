package app.ytsleep.youtube

sealed class ContentError : Exception() {
    data object Offline : ContentError()
    data object NotSignedIn : ContentError()
    data object SessionExpired : ContentError()
    data class Unplayable(val reason: String) : ContentError()
    data class SourceChanged(val detail: String) : ContentError()
}

interface ContentSource {
    suspend fun search(query: String, nextToken: String? = null): Page<MediaItem>
    suspend fun feed(kind: FeedKind, nextToken: String? = null): Page<MediaItem>
    suspend fun myPlaylists(): List<PlaylistSummary>
    suspend fun playlistItems(playlistId: String, nextToken: String? = null): Page<MediaItem>
    suspend fun resolveAudio(videoId: String, quality: AudioQuality): AudioStream
    suspend fun addToPlaylist(playlistId: String, videoId: String)
}
