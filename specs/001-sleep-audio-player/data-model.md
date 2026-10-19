# Data Model: Sleep Audio Player for YouTube

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md)

Everything is stored on the phone only (FR-025).
- **Persisted** entities are marked 💾.
- Everything else lives in memory and is rebuilt from YouTube.

## MediaItem (in memory)

A YouTube video that can be listed and played as audio.

| Field | Type | Notes |
|---|---|---|
| videoId | String | 11-character YouTube id; unique identity |
| title | String | |
| channelName | String | |
| channelId | String? | |
| thumbnailUrl | String | Highest resolution ≤ 480 px wide, for lists |
| durationSeconds | Int? | `null` for live or unknown |
| isMarkedShort | Boolean | From a Shorts renderer, a `/shorts/` URL, or the extractor's `isShortFormContent()` |
| isLive | Boolean | |

**Validation / rules**:
- **Shorts rule (FR-012)**: an item is a Short if `isMarkedShort || (durationSeconds != null &&
  durationSeconds <= 180)`. Shorts are removed before any list reaches the UI or the queue. An
  unknown duration alone never makes an item a Short.
- **Ads rule (FR-002)**: ad renderers never become `MediaItem`s (see research R3).
- **Live items**: shown, but marked with a "Live" label. If audio can't be resolved, playback
  shows the "can't play" message (spec Edge Cases).

## PlaylistSummary (in memory)

| Field | Type | Notes |
|---|---|---|
| playlistId | String | Includes the special ids `WL` (Watch later) and `LL` (Liked videos) |
| title | String | |
| thumbnailUrl | String? | |
| itemCount | Int? | As reported by YouTube; it may include Shorts that get hidden later |

## Page<T> (in memory)

A finite batch of items (FR-013).

| Field | Type | Notes |
|---|---|---|
| items | List<T> | Already filtered |
| nextToken | String? | `null` means the end. The UI shows "Load more" only when this is non-null |

## FeedKind (enum)

`HOME`, `SUBSCRIPTIONS`.

## PlaybackSession (in memory, owned by the playback service)

| Field | Type | Notes |
|---|---|---|
| queue | List<MediaItem> | One item (search, feed or recent) or a filtered playlist |
| sourcePlaylistId | String? | Set when the queue came from a playlist |
| currentIndex | Int | |
| positionMs | Long | |
| state | enum | `IDLE`, `RESOLVING`, `PLAYING`, `PAUSED`, `ENDED`, `ERROR` |
| repeatMode | RepeatMode | Loaded from settings |
| sleepTimer | SleepTimer? | |

**State transitions**:

```text
IDLE ─play(item|queue)─▶ RESOLVING ─url ok─▶ PLAYING ⇄ PAUSED (user, audio focus loss, timer end)
RESOLVING ─unplayable─▶ ERROR ─(in playlist)─▶ RESOLVING(next)   ─(single item)─▶ stays ERROR
PLAYING ─item ends─▶ repeat ONE: PLAYING(same, from 0)
                     next exists: RESOLVING(next)
                     last item & repeat PLAYLIST: RESOLVING(index 0)
                     last item & repeat OFF: ENDED
PLAYING ─stream URL expired (403/410)─▶ RESOLVING(same, keep position) ─▶ PLAYING
```

## RepeatMode (enum) 💾 in Settings

`OFF` (default), `ONE`, `PLAYLIST` (FR-006a).

## SleepTimer (in memory)

| Field | Type | Notes |
|---|---|---|
| kind | enum | `DURATION`, `END_OF_ITEM` |
| endsAtElapsedMs | Long? | For `DURATION`; based on the device's monotonic clock |
| fadeMs | Long | 10 000 |

**Presets**: 15, 30, 45, 60, 90 min, or end of current item.
- Cancelling the timer clears it.
- When the timer fires, volume fades to 0, then the player pauses and volume resets to 1.

## HistoryEntry 💾 (Room table `history`)

| Field | Type | Notes |
|---|---|---|
| videoId | String (PK) | |
| title | String | Cached for the offline start screen |
| channelName | String | |
| thumbnailUrl | String | |
| durationSeconds | Int? | |
| lastPositionMs | Long | Saved every 10 s while playing, plus on pause and stop |
| lastPlayedAt | Long (epoch ms) | Indexed, sorted descending for "recent" |
| sourcePlaylistId | String? | Lets "Continue listening" restore the playlist queue |
| sourceIndex | Int? | |

**Rules**:
- **Continue listening** = the newest entry. It resumes at `lastPositionMs`.
- An item played to within 30 s of its end is saved with `lastPositionMs = 0`, so it restarts
  next time.
- The start screen lists the 20 most recent entries. Entries beyond 500 are pruned (oldest
  first).

## Settings 💾 (DataStore `settings`)

| Key | Type | Default |
|---|---|---|
| repeatMode | RepeatMode | OFF |
| lastSleepTimerMinutes | Int? | null |
| batteryPromptShown | Boolean | false |
| lastUpdateCheckAt | Long | 0 |

## AccountConnection 💾 (DataStore `account`, excluded from backup)

| Field | Type | Notes |
|---|---|---|
| cookies | Map<String,String> | Only the YouTube/Google session cookies (research R4) |
| connectedAt | Long | |
| status | enum (derived) | `DISCONNECTED`, `CONNECTED`, `EXPIRED`. Set to `EXPIRED` when a signed-in request returns "not logged in" or 401/403 |

## AppVersion / UpdateInfo (in memory, from the remote manifest)

| Field | Type | Notes |
|---|---|---|
| installedVersionCode | Int | `BuildConfig.VERSION_CODE` |
| installedVersionName | String | Shown in Settings → About (FR-023) |
| available | UpdateManifest? | See [contracts/update-manifest.md](./contracts/update-manifest.md) |
| downloadState | enum | `NONE`, `DOWNLOADING`, `READY`, `INSTALLING`, `DISMISSED`, `FAILED`. `READY` with nothing playing opens the system install prompt automatically. `DISMISSED` means the user cancelled it, so the start screen shows the "Update ready" banner |

**Rule**: an update is offered only when `available.versionCode > installedVersionCode` and the
downloaded APK's SHA-256 matches the manifest.
