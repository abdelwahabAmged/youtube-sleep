# Contract: `ContentSource` (internal boundary to YouTube)

All YouTube access goes through this interface, so that the fragile parts can be fixed or
swapped without touching the UI or the player. The fragile parts are NewPipeExtractor
(research R2) and the InnerTube client (research R3).

Types refer to [data-model.md](../data-model.md).

```kotlin
interface ContentSource {
    /** Search results, Shorts and ads already removed. token = null for the first page. */
    suspend fun search(query: String, token: String? = null): Page<MediaItem>

    /** Requires a connected account. */
    suspend fun feed(kind: FeedKind, token: String? = null): Page<MediaItem>

    /** The user's playlists, including Watch later (WL) and Liked videos (LL) when available. Requires an account. */
    suspend fun myPlaylists(): List<PlaylistSummary>

    /** Playlist items with Shorts removed. Uses the account when connected (private playlists, WL, LL). */
    suspend fun playlistItems(playlistId: String, token: String? = null): Page<MediaItem>

    /** A fresh audio-only stream URL for playback. Never returns video formats. */
    suspend fun resolveAudio(videoId: String): AudioStream
}

data class AudioStream(
    val url: String,
    val mimeType: String,      // "audio/webm" (opus) preferred, otherwise "audio/mp4" (aac)
    val bitrate: Int,
    val contentLength: Long?,
    val expiresAtEpochMs: Long?,
)
```

## Guarantees (tested)

1. **No Shorts**: no `MediaItem` returned by `search`, `feed` or `playlistItems` meets the Shorts
   rule (`isMarkedShort` or `durationSeconds <= 180`).
2. **No ads**: renderers known to be ads never produce items. An unknown renderer is skipped, not
   turned into an item.
3. **Audio only**: `resolveAudio` only selects audio-only formats. If none exist it throws
   `ContentError.Unplayable`.
4. **Finite pages**: every list call returns one page. More pages are fetched only when the caller
   passes `nextToken`.

## Errors

```kotlin
sealed class ContentError : Exception() {
    object Offline : ContentError()
    object NotSignedIn : ContentError()              // account missing → UI shows "Connect account"
    object SessionExpired : ContentError()           // account EXPIRED → UI shows "Reconnect account"
    data class Unplayable(val reason: String) : ContentError()   // private/removed/age/region/live-only
    data class SourceChanged(val detail: String) : ContentError() // parser/extractor broke → "YouTube changed, update needed"
}
```

## Implementations

| Method | Implementation |
|---|---|
| search, resolveAudio | NewPipeExtractor |
| feed, myPlaylists | InnerTube (needs cookies) |
| playlistItems | InnerTube when signed in; otherwise NewPipeExtractor (public playlists only) |

A `FakeContentSource` backed by fixtures is used in UI and unit tests.
