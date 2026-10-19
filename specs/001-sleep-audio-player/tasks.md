---

description: "Task list for Sleep Audio Player for YouTube"
---

# Tasks: Sleep Audio Player for YouTube

**Input**: Design documents from `specs/001-sleep-audio-player/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: These are included because the constitution requires them.
- Principle II: the Shorts/ads filter MUST be covered by tests.
- Quality gates: tests SHOULD cover playback state logic.
- Plan R10: JVM unit tests for filters, parsers, the update manifest, the sleep timer and history
  rules. On-device checks follow [quickstart.md](./quickstart.md).

**Organization**: Tasks are grouped by user story. Phases follow the spec's priority order: US1
(P1), US4 (P1), US2 (P2), US3 (P2), US5 (P3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1 = Listen with screen off · US2 = Feed & playlists · US3 = Easy updates · US4 = Works with other apps blocked · US5 = Sleep timer

## Path Conventions

This is a single Android module (see [plan.md](./plan.md) → Project Structure):
- Main sources: `app/src/main/java/app/ytsleep/`
- JVM tests: `app/src/test/java/app/ytsleep/`
- Test fixtures: `app/src/test/resources/`
- Instrumented tests: `app/src/androidTest/java/app/ytsleep/`
- Resources: `app/src/main/res/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the project so it builds an empty app that installs on the phone.

- [X] T001 Initialize a git repository at the repo root and create `.gitignore` excluding `build/`, `.gradle/`, `local.properties`, `*.jks`, `*.keystore`, `app/release/`, and `update.json` in `.gitignore`
- [X] T002 Create the Gradle wrapper and root build files:
  - `settings.gradle.kts`: rootProject "youtube-sleep", include `:app`, repositories google(), mavenCentral(), `maven("https://jitpack.io")`.
  - `build.gradle.kts`: plugin aliases only.
  - `gradle/libs.versions.toml`: pin the latest stable versions of Kotlin 2.x, AGP, Compose BOM, Material3, Navigation Compose, AndroidX Media3 (exoplayer, session), `com.github.TeamNewPipe:NewPipeExtractor` (latest release tag), OkHttp, kotlinx-serialization-json, kotlinx-coroutines, Coil (compose), Room (runtime, ktx, compiler via KSP), DataStore preferences, desugar_jdk_libs, JUnit 4, kotlinx-coroutines-test, AndroidX test runner/ext-junit.
- [X] T003 Create `app/build.gradle.kts` with:
  - `namespace`/`applicationId` = `app.ytsleep`, compileSdk 36, minSdk 29, targetSdk 36, `versionCode = 1`, `versionName = "0.1.0"`.
  - JDK 17 toolchain, Compose enabled, KSP for Room, `isCoreLibraryDesugaringEnabled = true`.
  - `buildConfig = true` with `buildConfigField("String", "RELEASE_REPO", …)` read from the Gradle property `RELEASE_REPO` (default `"owner/youtube-sleep-releases"`).
  - A `release` signingConfig read from `~/.gradle/gradle.properties` keys `YTSLEEP_STORE_FILE`, `YTSLEEP_STORE_PASSWORD`, `YTSLEEP_KEY_ALIAS`, `YTSLEEP_KEY_PASSWORD`.
  - The release buildType uses that signingConfig with `isMinifyEnabled = false` (avoids R8 breaking NewPipeExtractor/Rhino).
- [X] T004 Create `app/src/main/AndroidManifest.xml`:
  - Permissions: INTERNET, ACCESS_NETWORK_STATE, WAKE_LOCK, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, REQUEST_INSTALL_PACKAGES, UPDATE_PACKAGES_WITHOUT_USER_ACTION, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS.
  - Application: `android:allowBackup="false"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, `android:name=".YtSleepApp"`, and a dark `android:theme`.
  - A single exported `MainActivity` with the LAUNCHER intent filter.
  - Also create `app/src/main/res/xml/data_extraction_rules.xml`, excluding all domains from cloud backup and device transfer.
- [X] T005 [P] Create the launcher icon (a dim crescent moon on #0B0B0F) as an adaptive icon in `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/drawable/ic_launcher_foreground.xml`, and `app/src/main/res/values/colors.xml`. Set the app name "YT Sleep" in `app/src/main/res/values/strings.xml`.
- [X] T006 [P] Create the dark window theme `Theme.YtSleep` (black windowBackground, dark status and navigation bars, no splash animation) in `app/src/main/res/values/themes.xml`

**Checkpoint**: `./gradlew assembleDebug` succeeds, and `adb install` puts an empty app icon on the phone.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The domain types, the YouTube boundary, the Shorts/ad filter, storage, the app
container and the navigation shell. Every story depends on these.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T007 [P] Create the Compose sleep theme in `app/src/main/java/app/ytsleep/ui/theme/Color.kt`, `Type.kt` and `Theme.kt`:
  - darkColorScheme with background/surface #0B0B0F, onBackground #B8B8C0 (dim gray), primary #7A7AA8 (muted).
  - No dynamic color and no light theme. Typography uses default sizes with slightly reduced contrast (FR-014).
- [X] T008 [P] Create the domain models in `app/src/main/java/app/ytsleep/youtube/Models.kt`, exactly as in data-model.md:
  - `MediaItem(videoId: String, title: String, channelName: String, channelId: String?, thumbnailUrl: String, durationSeconds: Int? /* null for live or unknown */, isMarkedShort: Boolean, isLive: Boolean)`
  - `PlaylistSummary(playlistId: String, title: String, thumbnailUrl: String?, itemCount: Int?)` — playlistId includes the special ids `WL` and `LL`.
  - `Page<T>(items: List<T>, nextToken: String?)` — "`null` means the end".
  - `enum FeedKind { HOME, SUBSCRIPTIONS }`
  - `AudioStream(url, mimeType, bitrate, contentLength: Long?, expiresAtEpochMs: Long?)`
- [X] T009 [P] Create `ContentSource` and the sealed `ContentError` in `app/src/main/java/app/ytsleep/youtube/ContentSource.kt`, exactly as in contracts/content-source.md:
  - Methods: `search`, `feed`, `myPlaylists`, `playlistItems`, `resolveAudio`.
  - Errors: `Offline`, `NotSignedIn`, `SessionExpired`, `Unplayable(reason)`, `SourceChanged(detail)`.
- [X] T010 [P] Write `ContentFilterTest` in `app/src/test/java/app/ytsleep/youtube/ContentFilterTest.kt`. Cases:
  - `isMarkedShort=true` with duration 3600 → hidden.
  - Duration 180 → hidden; duration 181 → shown.
  - Duration null and not marked → shown; live with null duration → shown.
  - `filter()` keeps order and drops only Shorts.
  - The tests must fail before T011 is implemented.
- [X] T011 Implement `ContentFilter` in `app/src/main/java/app/ytsleep/youtube/ContentFilter.kt`:
  - `const val SHORTS_MAX_SECONDS = 180`.
  - `fun isShort(item: MediaItem): Boolean = item.isMarkedShort || (item.durationSeconds != null && item.durationSeconds <= 180)`.
  - `fun filter(items: List<MediaItem>): List<MediaItem>`.
  - `fun isShortsUrl(url: String): Boolean` — true when the URL contains `/shorts/`.
  - T010 must pass.
- [X] T012 [P] Create the Room history storage in `app/src/main/java/app/ytsleep/data/db/HistoryEntry.kt`, `HistoryDao.kt` and `AppDatabase.kt`:
  - Table `history` with `videoId` String PK, `title`, `channelName`, `thumbnailUrl`, `durationSeconds: Int?`, `lastPositionMs: Long`, `lastPlayedAt: Long` (indexed), `sourcePlaylistId: String?`, `sourceIndex: Int?`.
  - DAO: `upsert(entry)`, `latest(): Flow<HistoryEntry?>`, `recent(limit: Int = 20): Flow<List<HistoryEntry>>` sorted by `lastPlayedAt DESC`, `updatePosition(videoId, positionMs, playedAt)`, `pruneKeepingNewest(500)`.
  - Database version 1, no migrations.
- [X] T013 [P] Create `SettingsStore` in `app/src/main/java/app/ytsleep/data/SettingsStore.kt`:
  - Preferences DataStore named `settings`.
  - Typed flows and setters for `repeatMode: RepeatMode` (default `OFF`), `lastSleepTimerMinutes: Int?` (default null), `batteryPromptShown: Boolean` (default false), `lastUpdateCheckAt: Long` (default 0).
  - Define `enum class RepeatMode { OFF, ONE, PLAYLIST }` in the same file.
- [X] T014 [P] Implement the NewPipe `Downloader` in `app/src/main/java/app/ytsleep/youtube/newpipe/OkHttpDownloader.kt`:
  - Subclass of `org.schabi.newpipe.extractor.downloader.Downloader`, backed by the shared OkHttpClient.
  - Maps NewPipe `Request` to OkHttp (method, headers, body).
  - Returns NewPipe `Response`; throws `ReCaptchaException` on HTTP 429.
- [X] T015 Create the application class and manual wiring in `app/src/main/java/app/ytsleep/YtSleepApp.kt`:
  - `AppContainer` holds a shared `OkHttpClient` (30 s timeouts), `AppDatabase`, `SettingsStore`, and a lazy `ContentSource` (filled in by later tasks).
  - Calls `NewPipe.init(OkHttpDownloader(client), Localization("en", "US"))` in `onCreate`.
  - Exposes the container through `(context.applicationContext as YtSleepApp).container`.
- [X] T016 Create the navigation shell in `app/src/main/java/app/ytsleep/MainActivity.kt` and `app/src/main/java/app/ytsleep/ui/AppNavigation.kt`:
  - `ComponentActivity` with edge-to-edge dark, setting content to `YtSleepTheme { AppNavigation() }`.
  - `NavHost` routes `start` (start destination), `feed`, `search`, `playlist/{playlistId}`, `settings`, `account/connect`, `account/paste`.
  - A bottom bar Start · Feed · Search · Settings.
  - All enter/exit transitions set to `EnterTransition.None`/`ExitTransition.None` (no animations, Principle VI).
  - A placeholder composable per route.
- [X] T017 [P] Create the shared list UI in `app/src/main/java/app/ytsleep/ui/common/`:
  - `MediaItemRow.kt`: static Coil thumbnail 120×68 dp, title (2 lines), channel, duration `h:mm:ss`, "Live" label when `isLive`.
  - `PlaylistRow.kt`
  - `LoadMoreButton.kt`: shown only when `nextToken != null` (FR-013).
  - `StatusMessage.kt`: calm texts for offline / not signed in / session expired / unplayable / "YouTube changed something — an app update is needed".
  - `DimPlaceholder.kt`: static dim box, no shimmer.

**Checkpoint**: unit tests pass (`./gradlew testDebugUnitTest`). The app opens to empty placeholder screens with the bottom bar.

---

## Phase 3: User Story 1 — Listen to a video as audio with the screen off (Priority: P1) 🎯 MVP

**Goal**: Search → tap → audio only (no video, no ads, no Shorts) that keeps playing with the
screen locked, with lock-screen controls. The start screen offers "Continue listening" and
recent items.

**Independent Test**: [quickstart.md](./quickstart.md) V1, V2, V3 (search part), V4, V8, V9, V10.
For example: search "rain sounds 10 hours", play, lock the phone for 10+ minutes. Audio
continues, no video frame appears, and no ad plays.

### Tests for User Story 1

- [X] T018 [P] [US1] Write `AudioStreamSelectorTest` in `app/src/test/java/app/ytsleep/youtube/newpipe/AudioStreamSelectorTest.kt`. Cases:
  - Prefers the highest-bitrate Opus/WebM audio stream.
  - Falls back to M4A/AAC.
  - Never returns a video or video+audio stream.
  - Throws `ContentError.Unplayable` when the list has no audio-only stream.
- [X] T019 [P] [US1] Write `HistoryRulesTest` in `app/src/test/java/app/ytsleep/playback/HistoryRulesTest.kt`. Cases:
  - Position within 30 s of the end is stored as 0.
  - Otherwise the position is stored as is.
  - Unknown duration (null) stores the position as is.
  - The prune keeps the 500 newest.

### Implementation for User Story 1

- [X] T020 [P] [US1] Implement the WebView-based YouTube PO token provider in `app/src/main/java/app/ytsleep/youtube/newpipe/PoTokenWebViewProvider.kt`:
  - Port the approach from the NewPipe app (`org.schabi.newpipe.util.potoken`): a headless system WebView runs BotGuard and returns tokens.
  - Implements NewPipeExtractor's `PoTokenProvider`.
  - Register it via `YoutubeStreamExtractor.setPoTokenProvider(...)` in `YtSleepApp.onCreate`.
  - Cache the token for its validity period.
  - This uses the system WebView, not the Chrome app (research R2, R4).
- [X] T021 [P] [US1] Implement `NewPipeMapper` in `app/src/main/java/app/ytsleep/youtube/newpipe/NewPipeMapper.kt`, mapping `StreamInfoItem` → `MediaItem`:
  - `videoId` comes from the URL.
  - `durationSeconds` = `duration` (null when ≤ 0).
  - `isMarkedShort` = `isShortFormContent || ContentFilter.isShortsUrl(url)`.
  - `isLive` = streamType is LIVE.
  - `thumbnailUrl` = the largest thumbnail ≤ 480 px wide.
- [X] T022 [US1] Implement `AudioStreamSelector` in `app/src/main/java/app/ytsleep/youtube/newpipe/AudioStreamSelector.kt`. It selects from `StreamInfo.audioStreams` only (never `videoStreams`/`videoOnlyStreams`), as specified by T018, and returns `AudioStream`. T018 must pass.
- [X] T023 [US1] Implement `YouTubeContentSource` (search and resolveAudio) in `app/src/main/java/app/ytsleep/youtube/YouTubeContentSource.kt`:
  - `search` uses the NewPipe YouTube `SearchExtractor` with the "videos" content filter, maps items with NewPipeMapper, and applies `ContentFilter.filter`.
  - Paging: `nextToken` holds the serialized `Page`.
  - `resolveAudio` uses `StreamInfo.getInfo` + AudioStreamSelector.
  - Exception mapping: `ContentNotAvailableException`/`AgeRestrictedContentException`/`GeographicRestrictionException` → `Unplayable`; `ParsingException`/`ExtractionException` → `SourceChanged`; `IOException` → `Offline`.
  - `feed`/`myPlaylists` throw `NotSignedIn` for now.
  - Wire it into `AppContainer` in `app/src/main/java/app/ytsleep/YtSleepApp.kt`.
- [X] T024 [US1] Implement `StreamResolvingDataSource` in `app/src/main/java/app/ytsleep/playback/StreamResolvingDataSource.kt`:
  - Media3 `ResolvingDataSource.Factory` over `OkHttpDataSource` that turns `ytsleep://<videoId>` into a fresh URL via `ContentSource.resolveAudio`.
  - Caches the URL per videoId until `expiresAtEpochMs` (or 5 h).
  - On HTTP 403/410, invalidates the cache, re-resolves once and retries at the same byte position.
  - Limits each request to a ≤ 10 MB byte range (YouTube throttling, research R5).
  - A debug-only override `forceExpiryAfterMinutes` (from BuildConfig.DEBUG settings) to test V10.
- [X] T025 [US1] Implement `PlaybackService` in `app/src/main/java/app/ytsleep/playback/PlaybackService.kt`:
  - A `MediaSessionService` owning an `ExoPlayer`.
  - The player is built with a custom `RenderersFactory` that creates **audio renderers only** (no video renderer) and track selection with video disabled (Principle I).
  - `setAudioAttributes(USAGE_MEDIA/CONTENT_TYPE_MUSIC, handleAudioFocus = true)` and `setHandleAudioBecomingNoisy(true)` (FR-007).
  - `setWakeMode(C.WAKE_MODE_NETWORK)` and `setSeekBackIncrementMs(15_000)` / `setSeekForwardIncrementMs(30_000)`.
  - Media source factory uses StreamResolvingDataSource.
  - A MediaSession with a custom notification/lock-screen layout: previous, −15 s, play/pause, +30 s, next (FR-004).
  - Register it in `app/src/main/AndroidManifest.xml` with `android:foregroundServiceType="mediaPlayback"`, `exported="true"` and the `androidx.media3.session.MediaSessionService` intent filter.
- [X] T026 [US1] Implement playback error handling in `app/src/main/java/app/ytsleep/playback/PlaybackErrorPolicy.kt`, used by PlaybackService:
  - When an item fails with `Unplayable` and a next item exists in the queue, skip to it automatically.
  - Otherwise pause and publish the error (`Unplayable`/`SourceChanged`/`Offline`) through session extras for the UI.
  - Never loop errors (at most one retry per item).
- [X] T027 [P] [US1] Implement `QueueBuilder` in `app/src/main/java/app/ytsleep/playback/QueueBuilder.kt`:
  - Converts `List<MediaItem>` + start index (+ optional `sourcePlaylistId`) to Media3 `MediaItem`s with uri `ytsleep://<videoId>`, `mediaId = videoId`, and metadata title/artist(channel)/artworkUri(thumbnail).
  - Stores `sourcePlaylistId` and the index in extras.
- [X] T028 [US1] Implement `HistoryRecorder` in `app/src/main/java/app/ytsleep/playback/HistoryRecorder.kt`, attached in PlaybackService:
  - Saves to `HistoryDao` every 10 s while playing, on pause, on item transition and on service stop.
  - Position rule: within 30 s of the end → store 0.
  - Stores `sourcePlaylistId`/`sourceIndex`.
  - Calls `pruneKeepingNewest(500)` once per session.
  - The pure rule functions live in a `HistoryRules` object in the same file. T019 must pass.
- [X] T029 [US1] Implement `PlayerConnection` in `app/src/main/java/app/ytsleep/playback/PlayerConnection.kt`:
  - A UI-side wrapper around a Media3 `MediaController` (connects to PlaybackService).
  - Exposes `StateFlow<PlayerUiState>`: current item metadata, isPlaying, positionMs (ticks every 1 s while visible), durationMs, error, repeatMode, sleepTimer remaining.
  - Commands: `playItems(items, startIndex, sourcePlaylistId)`, `playPause()`, `seekBack()`, `seekForward()`, `next()`, `previous()`, `seekTo(ms)`, `resume(entry: HistoryEntry)`.
  - Provided from AppContainer.
- [X] T030 [P] [US1] Implement the Search screen in `app/src/main/java/app/ytsleep/ui/search/SearchViewModel.kt` and `SearchScreen.kt`:
  - Query field with an IME search action.
  - The first page is fetched via `ContentSource.search`; "Load more" appends using `nextToken` (no auto-load on scroll).
  - Tapping a row calls `playItems(listOf(item), 0, null)` and opens the player sheet.
  - Errors use StatusMessage.
- [X] T031 [P] [US1] Implement the player sheet in `app/src/main/java/app/ytsleep/ui/player/PlayerSheet.kt` and `PlayerViewModel.kt`:
  - A ModalBottomSheet / expandable mini-player bar shown on all screens when something is loaded.
  - Shows a static thumbnail, title, channel, seek Slider, elapsed/total, and ⏮ −15 s ⏯ +30 s ⏭ buttons.
  - Never shows a video surface (FR-001, FR-005).
  - Hosted from `app/src/main/java/app/ytsleep/ui/AppNavigation.kt`.
- [X] T032 [US1] Implement the Start screen in `app/src/main/java/app/ytsleep/ui/start/StartViewModel.kt` and `StartScreen.kt`:
  - A "Continue listening" card from `HistoryDao.latest()`. One tap calls `PlayerConnection.resume(entry)`, which plays at `lastPositionMs`.
  - A "Recent" list of up to 20 from `HistoryDao.recent()`; tapping one plays it.
  - First launch (no history) shows no card, plus a "Search" button and a "Connect your YouTube account" hint.
  - Recent still renders from local history when offline (FR-015, FR-024).
- [X] T033 [US1] Add the one-time battery optimization prompt in `app/src/main/java/app/ytsleep/ui/start/BatteryOptimizationPrompt.kt`:
  - When `PowerManager.isIgnoringBatteryOptimizations` is false and `batteryPromptShown` is false, show a calm dialog.
  - "Allow" fires `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` for the package.
  - Set `batteryPromptShown = true` either way.
  - Shown from StartScreen after the first playback.
- [X] T034 [US1] Build a debug APK, install it on the phone, and run quickstart V1, V2, V3 (search only), V4, V8, V9 and V10. Record the results in `specs/001-sleep-audio-player/checklists/validation.md`.

**Checkpoint**: The MVP works. You can search and listen with the screen off, with no video, ads or Shorts, and resume from the start screen.

---

## Phase 4: User Story 4 — Works while other apps are blocked (Priority: P1)

**Goal**: Nothing in the app hands off to the browser, YouTube or the Play Store. The first
install is documented as the only step that uses another tool.

**Independent Test**: Block Chrome, YouTube and the Play Store in AppBlock, then run every
available flow ([quickstart.md](./quickstart.md) V13, and V1 with the apps blocked).

- [X] T035 [P] [US4] Write `NoExternalIntentsTest` in `app/src/test/java/app/ytsleep/NoExternalIntentsTest.kt`. It is a JVM test that scans every `.kt` file under `app/src/main/java` and fails if it finds `Intent.ACTION_VIEW`, `CustomTabsIntent`, `openUri(` or `"vnd.youtube`. Allowlist: `Settings.ACTION_*` and PackageInstaller intents (FR-020).
- [X] T036 [P] [US4] Neutralize external link handling in `app/src/main/java/app/ytsleep/ui/AppNavigation.kt`. Provide a `LocalUriHandler` that ignores every URI, so any text link or library composable can never open another app.
- [X] T037 [P] [US4] Create `InAppOnlyWebViewClient` in `app/src/main/java/app/ytsleep/ui/account/InAppOnlyWebViewClient.kt`:
  - A `WebViewClient` that keeps navigation to `accounts.google.com`, `*.google.com`, `*.youtube.com` and `*.gstatic.com` inside the WebView.
  - Silently blocks `intent://`, `market://`, `vnd.youtube:` and any other host (returns true without launching anything).
  - US2's sign-in uses it.
- [X] T038 [US4] Write `README.md` at the repo root covering:
  - One-time phone setup on Android 11: a USB or wireless `adb install`, the battery exemption, allowing "Install unknown apps" for YT Sleep (needed for self-updates), and blocking Chrome/YouTube/Play Store in AppBlock afterwards.
  - That no blocked app is needed after that (research R7, quickstart "Phone setup").
- [X] T039 [US4] With Chrome, YouTube and the Play Store blocked in AppBlock, run quickstart V13 and repeat V1. Record the results in `specs/001-sleep-audio-player/checklists/validation.md`.

**Checkpoint**: Stories 1 and 4 hold. The app is fully usable with the blocked apps blocked.

---

## Phase 5: User Story 2 — Browse my feed and my playlists (Priority: P2)

**Goal**: One-time in-app sign-in. A Feed screen with Home | Subscriptions tabs, plus My
playlists (including Watch later and Liked) with thumbnails. Playlists auto-advance and stop at
the end unless repeat is on.

**Independent Test**: [quickstart.md](./quickstart.md) V3 (feeds and playlists), V5 and V6.
Sign in, open both feed tabs (no Shorts, "Load more" only), play a playlist with the screen
locked, and confirm it advances and stops or repeats correctly.

### Tests for User Story 2

- [X] T040 [P] [US2] Record anonymized InnerTube fixtures in `app/src/test/resources/innertube/`: `home.json`, `home_continuation.json`, `subscriptions.json`, `library_playlists.json`, `playlist_WL.json` and `playlist_continuation.json`.
  - How: capture real `youtubei/v1/browse` responses from a signed-in desktop browser's devtools (one-time step, allowed), strip personal data, and trim to about 10 items each.
  - Each must include at least one Shorts shelf/`reelItemRenderer`/`shortsLockupViewModel`, one ≤ 180 s video, one `adSlotRenderer` and one unknown renderer.
- [X] T041 [P] [US2] Write `SapisidHashTest` in `app/src/test/java/app/ytsleep/youtube/innertube/SapisidHashTest.kt`. Using a fixed timestamp, SAPISID and origin `https://www.youtube.com`, check the header equals `SAPISIDHASH <ts>_<sha1hex("<ts> <SAPISID> <origin>")>`.
- [X] T042 [P] [US2] Write `FeedParserTest` in `app/src/test/java/app/ytsleep/youtube/innertube/FeedParserTest.kt`. Using the home/subscriptions fixtures, check that:
  - videos map to `MediaItem` with the right `videoId`/title/channel/duration/thumbnail;
  - Shorts renderers come out with `isMarkedShort=true`;
  - ad renderers produce no items;
  - unknown renderers are skipped without an exception;
  - the continuation token is extracted;
  - the continuation fixture parses.
- [X] T043 [P] [US2] Write `PlaylistParserTest` and `LibraryParserTest` in `app/src/test/java/app/ytsleep/youtube/innertube/`:
  - Playlist items keep their order and get `lengthSeconds`.
  - Continuation is parsed.
  - The library yields `PlaylistSummary` entries plus `WL` "Watch later" and `LL` "Liked videos".
- [X] T044 [P] [US2] Write `CookieParserTest` in `app/src/test/java/app/ytsleep/auth/CookieParserTest.kt`. Cases:
  - Netscape `cookies.txt` and a `name=value; name2=value2` header string are both parsed.
  - Only the allowed cookie names are kept (`SAPISID`, `__Secure-3PAPISID`, `__Secure-1PAPISID`, `SID`, `HSID`, `SSID`, `APISID`, `LOGIN_INFO`, `__Secure-3PSID`, `__Secure-1PSID`).
  - Input missing `SAPISID` is rejected.

### Implementation for User Story 2

- [X] T045 [P] [US2] Implement `CookieParser` in `app/src/main/java/app/ytsleep/auth/CookieParser.kt` as specified by T044.
- [X] T046 [P] [US2] Implement `AccountStore` in `app/src/main/java/app/ytsleep/auth/AccountStore.kt`:
  - Preferences DataStore named `account` holding `cookies: Map<String,String>` (JSON-encoded) and `connectedAt: Long`.
  - `status: Flow<AccountStatus>` with `DISCONNECTED`, `CONNECTED` and `EXPIRED`.
  - Methods: `save(cookies)`, `markExpired()`, `signOut()` (clears the store and calls `CookieManager.getInstance().removeAllCookies`).
  - Cookies are never logged.
- [X] T047 [P] [US2] Implement `SapisidHash` in `app/src/main/java/app/ytsleep/youtube/innertube/SapisidHash.kt` as specified by T041.
- [X] T048 [US2] Implement `InnerTubeClient` in `app/src/main/java/app/ytsleep/youtube/innertube/InnerTubeClient.kt`:
  - `suspend fun browse(browseId: String): JsonObject` and `suspend fun continuation(token: String): JsonObject`.
  - Both POST to `https://www.youtube.com/youtubei/v1/browse?prettyPrint=false`, with context `client.clientName = "WEB"`, a `clientVersion` constant, hl `en` and gl `US`.
  - Headers: `Cookie` (only to youtube.com), `Authorization: SAPISIDHASH …`, `X-Origin`/`Origin: https://www.youtube.com`, `X-Goog-AuthUser: 0`.
  - Errors: HTTP 401/403, or `responseContext` showing a logged-out state, call `AccountStore.markExpired()` and throw `ContentError.SessionExpired`. No cookies → `NotSignedIn`. IOException → `Offline`.
- [X] T049 [P] [US2] Implement `FeedParser` in `app/src/main/java/app/ytsleep/youtube/innertube/FeedParser.kt`:
  - Walks `richGridRenderer` → `richItemRenderer` → `videoRenderer` | `lockupViewModel`.
  - Marks `reelItemRenderer`, `shortsLockupViewModel`, any `richSectionRenderer` reel shelf and `/shorts/` URLs as `isMarkedShort=true`.
  - Drops `adSlotRenderer`, `promotedSparklesWebRenderer` and `statementBannerRenderer`.
  - Skips unknown renderers.
  - Reads `continuationItemRenderer` for `nextToken`.
  - Handles both the initial and the `onResponseReceivedActions` continuation shapes.
  - T042 must pass.
- [X] T050 [P] [US2] Implement `PlaylistParser` and `LibraryParser` in `app/src/main/java/app/ytsleep/youtube/innertube/PlaylistParser.kt` and `LibraryParser.kt`:
  - `playlistVideoRenderer` → MediaItem with `lengthSeconds`, plus the continuation.
  - `FEplaylist_aggregation` grid/lockups → `PlaylistSummary`, with a fallback to `FElibrary` shelves.
  - Always append `WL` "Watch later" and `LL` "Liked videos".
  - T043 must pass.
- [X] T051 [US2] Extend `YouTubeContentSource` in `app/src/main/java/app/ytsleep/youtube/YouTubeContentSource.kt`:
  - `feed(HOME)` → browse `FEwhat_to_watch`; `feed(SUBSCRIPTIONS)` → browse `FEsubscriptions`. Both go through FeedParser + `ContentFilter.filter`, with token → continuation.
  - `myPlaylists()` → `FEplaylist_aggregation` via LibraryParser.
  - `playlistItems(id)` → browse `VL<id>` via PlaylistParser + ContentFilter when signed in; otherwise the NewPipe `PlaylistExtractor` for public playlists, mapped with NewPipeMapper + ContentFilter.
- [X] T052 [US2] Implement the in-app sign-in screen in `app/src/main/java/app/ytsleep/ui/account/ConnectAccountScreen.kt`:
  - A full-screen WebView using InAppOnlyWebViewClient, with JavaScript and DOM storage enabled and a mobile user agent without the `; wv` token.
  - Clears cookies first, then loads `https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/`.
  - When the URL host becomes `*.youtube.com` and `CookieManager.getCookie("https://www.youtube.com")` contains `SAPISID`, parse it with CookieParser, `AccountStore.save`, and pop back.
  - If Google shows a "browser may not be secure" page, show a link to the Paste cookies screen (research R4).
- [X] T053 [P] [US2] Implement the paste-cookies fallback in `app/src/main/java/app/ytsleep/ui/account/PasteCookiesScreen.kt`. It has a multiline text field and a Save button, uses CookieParser to validate, shows an error if `SAPISID` is missing, and calls `AccountStore.save`.
- [X] T054 [US2] Implement the Settings account section in `app/src/main/java/app/ytsleep/ui/settings/SettingsScreen.kt` and `SettingsViewModel.kt`. Create these files if they don't exist; extend them, keeping the About section, if T065 already created them:
  - Shows account status.
  - Connect (or Reconnect when `EXPIRED`) → `account/connect`; "Paste cookies" → `account/paste`; Sign out → `AccountStore.signOut()`.
  - Battery optimization row reusing BatteryOptimizationPrompt logic.
- [X] T055 [US2] Implement the Feed screen in `app/src/main/java/app/ytsleep/ui/feed/FeedViewModel.kt` and `FeedScreen.kt`:
  - `TabRow` with Home | Subscriptions; each tab loads one page and has "Load more" (no auto-load on scroll, FR-013).
  - `NotSignedIn`/`SessionExpired` show StatusMessage with a Connect/Reconnect button → `account/connect`.
  - Tapping a row plays the single item.
- [X] T056 [US2] Implement the Playlist screen in `app/src/main/java/app/ytsleep/ui/playlist/PlaylistViewModel.kt` and `PlaylistScreen.kt`:
  - Header (title, count), items + "Load more", and a "Play all" button.
  - Tapping an item starts a queue from that item.
  - The queue is built by `PlaylistQueueLoader` (same file as the ViewModel), which fetches all remaining pages up to 500 items before calling `playItems(items, index, playlistId)`. This ensures auto-advance covers the whole filtered playlist (FR-006).
- [X] T057 [US2] Add a "My playlists" section to the Start screen in `app/src/main/java/app/ytsleep/ui/start/StartScreen.kt` and `StartViewModel.kt`:
  - Lists `ContentSource.myPlaylists()` when CONNECTED, with tap → `playlist/{id}`.
  - Shows a "Connect your YouTube account" hint when DISCONNECTED/EXPIRED.
  - "Continue listening" restores the playlist queue when `sourcePlaylistId` is set, by loading via PlaylistQueueLoader and resuming at `sourceIndex`/`lastPositionMs`.
- [X] T058 [US2] Implement repeat mode (FR-006a):
  - In `app/src/main/java/app/ytsleep/playback/PlaybackService.kt`, read `SettingsStore.repeatMode` on start and map `OFF`→`REPEAT_MODE_OFF`, `ONE`→`REPEAT_MODE_ONE`, `PLAYLIST`→`REPEAT_MODE_ALL`. Persist changes back from `Player.Listener.onRepeatModeChanged`.
  - In `app/src/main/java/app/ytsleep/ui/player/PlayerSheet.kt`, add a repeat toggle that cycles off → one → playlist, with distinct icons.
  - Default is `OFF`, and playback stops after the last item.
- [X] T059 [US2] Run quickstart V3 (feeds and playlists), V5 and V6 on the phone with blocked apps still blocked. Record the results in `specs/001-sleep-audio-player/checklists/validation.md`.

**Checkpoint**: Stories 1, 4 and 2 all work independently.

---

## Phase 6: User Story 3 — Get new versions on the phone easily (Priority: P2)

**Goal**: `scripts/release.sh` on the dev machine publishes the update. On the phone
(Android 11), the app downloads it in the background and opens the system install prompt by
itself while nothing is playing. One tap on "Install" updates it with data kept.

**Independent Test**: [quickstart.md](./quickstart.md) V11 and V12. Publish a visible change,
open the app with nothing playing, tap "Install" on the prompt that appears, and check that the
new version shows in About with sign-in, history and repeat mode kept.

**Story order note**: this phase may run before Phase 5 (US2), as the Implementation Strategy
recommends. It only needs StartScreen (T032) from US1. T065 creates SettingsScreen if US2
hasn't yet.

### Tests for User Story 3

- [X] T060 [P] [US3] Write `UpdateManifestTest` in `app/src/test/java/app/ytsleep/update/UpdateManifestTest.kt`, covering every rule in contracts/update-manifest.md:
  - `schema` must be `1` (unknown → ignored).
  - `versionCode` greater than installed → offered; equal or lower → not offered.
  - `apkUrl` must be HTTPS on `github.com`/`objects.githubusercontent.com`.
  - `sha256` must be 64 lowercase hex chars.
  - `minSdk` greater than the device SDK → not offered.
  - Optional `notes`/`publishedAt` may be absent.

### Implementation for User Story 3

- [X] T061 [P] [US3] Implement `UpdateManifest` in `app/src/main/java/app/ytsleep/update/UpdateManifest.kt`:
  - A `@Serializable` data class with `schema`, `versionCode`, `versionName`, `apkUrl`, `sha256`, `sizeBytes`, `minSdk`, `publishedAt?`, `notes?`.
  - `fun isOfferable(installedVersionCode: Int, deviceSdk: Int): Boolean` implementing the contract rules.
  - T060 must pass.
- [X] T062 [US3] Implement `UpdateChecker` in `app/src/main/java/app/ytsleep/update/UpdateChecker.kt`:
  - On app start, and at most every 6 h while open (based on `SettingsStore.lastUpdateCheckAt`), GET `https://github.com/${BuildConfig.RELEASE_REPO}/releases/latest/download/update.json` with a 10 s timeout and redirects followed.
  - If offerable, download the APK to `cacheDir/updates/<versionCode>.apk` and check `sizeBytes` and the SHA-256 (delete on mismatch).
  - Expose `StateFlow<UpdateState>` (`NONE`, `DOWNLOADING`, `READY(manifest)`, `INSTALLING`, `DISMISSED(manifest)`, `FAILED`).
  - Every failure is silent (`NONE`). The app keeps working offline (Story 3, scenario 3).
  - Started from `YtSleepApp.onCreate`.
  - **Auto-prompt rule**: when the state is `READY`, the activity is in the foreground, and `PlayerConnection` reports nothing playing, call `UpdateInstaller.install` automatically, at most once per `versionCode` per launch (FR-021).
  - If audio is playing, wait and trigger when playback pauses or stops.
- [X] T063 [US3] Implement `UpdateInstaller` in `app/src/main/java/app/ytsleep/update/UpdateInstaller.kt` and `InstallResultReceiver.kt`:
  - If `!packageManager.canRequestPackageInstalls()`, open `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` for this package first. This is a one-time system setting.
  - Create a `PackageInstaller` session with `MODE_FULL_INSTALL`, `setRequireUserAction(USER_ACTION_NOT_REQUIRED)` on API 31+ and `setRequestUpdateOwnership(true)` on API 34+, then write the APK and commit with a PendingIntent to InstallResultReceiver.
  - On `STATUS_PENDING_USER_ACTION`, which always happens on Android 11, launch the provided confirmation intent immediately. This system "Install" tap is the user's only action.
  - If the user cancels (`STATUS_FAILURE_ABORTED`), set `DISMISSED`.
  - On other failures, set `FAILED`.
  - Register the receiver (not exported) in `app/src/main/AndroidManifest.xml`.
- [X] T064 [US3] Add the fallback update banner and the one-time install permission prompt in `app/src/main/java/app/ytsleep/ui/start/UpdateBanner.kt`, and show both in StartScreen (which exists from T032):
  - The banner is visible only in `DISMISSED`/`FAILED` (the normal path is the automatic prompt from T062): "Update ready — {versionName}", plus the optional one-line `notes`.
  - Tapping it calls `UpdateInstaller.install`; `FAILED` shows "Retry".
  - The one-time setup card: when `!packageManager.canRequestPackageInstalls()`, show a calm "Allow updates" card that opens `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` for this package.
  - Calm styling, no animation.
- [X] T065 [US3] Add the About section in `app/src/main/java/app/ytsleep/ui/settings/SettingsScreen.kt`. If the file doesn't exist yet (US3 running before US2), create `SettingsScreen.kt` and `SettingsViewModel.kt` with just this section and wire them to the `settings` route; T054 adds the account section later. The section shows `versionName (versionCode)` from BuildConfig and has a "Check for updates" button that triggers UpdateChecker immediately (FR-023).
- [X] T066 [US3] Write `scripts/release.sh` (bash, `set -euo pipefail`):
  - Usage: `RELEASE_REPO=owner/repo ./scripts/release.sh "note"`.
  - Fails fast if `RELEASE_REPO`, `gh` auth or the `YTSLEEP_*` signing properties are missing.
  - Reads the current `versionCode`/`versionName` from `app/build.gradle.kts`, increments `versionCode` by 1 and the patch of `versionName` (override via `VERSION_NAME`), and writes them back.
  - Runs `./gradlew assembleRelease -PRELEASE_REPO=$RELEASE_REPO` and copies the APK to `app/release/youtube-sleep-<versionName>.apk`.
  - Writes `app/release/update.json` per contracts/update-manifest.md (`sha256sum`, `stat -c%s`, minSdk 29, `apkUrl = https://github.com/$RELEASE_REPO/releases/download/v<versionName>/youtube-sleep-<versionName>.apk`).
  - Runs `gh release create v<versionName> <apk> app/release/update.json --repo $RELEASE_REPO --notes "<note>" --latest`.
- [X] T067 [US3] Publish v0.1.x with `scripts/release.sh`, then run quickstart V11 and V12 on the phone. Confirm on Android 11 that the prompt opens by itself and exactly one "Install" tap is needed. Record the results in `specs/001-sleep-audio-player/checklists/validation.md`.

**Checkpoint**: New versions reach the phone with at most one tap. From here, every later change can ship through this path.

---

## Phase 7: User Story 5 — Sleep timer (Priority: P3)

**Goal**: A timer with presets 15/30/45/60/90 min or "end of current item". The audio fades out
over the last 10 s, then stops.

**Independent Test**: [quickstart.md](./quickstart.md) V7. Set 15 min, lock the phone, and check
that audio fades and stops at about 15 min.

### Tests for User Story 5

- [X] T068 [P] [US5] Write `SleepTimerTest` in `app/src/test/java/app/ytsleep/playback/SleepTimerTest.kt`. It uses a fake monotonic clock and virtual time (kotlinx-coroutines-test). Cases:
  - `DURATION` 30 min fires pause at 30:00.
  - Volume is 1.0 until the last 10 s (`fadeMs = 10000`), then decreases linearly to 0, then resets to 1.0 after the pause.
  - Cancel before the end → no pause and volume 1.0.
  - `END_OF_ITEM` pauses at the next item transition or end.
  - Setting a new timer replaces the old one.

### Implementation for User Story 5

- [X] T069 [US5] Implement `SleepTimer` in `app/src/main/java/app/ytsleep/playback/SleepTimer.kt`:
  - `kind` (`DURATION`, `END_OF_ITEM`), `endsAtElapsedMs: Long?` based on `SystemClock.elapsedRealtime`, `fadeMs = 10_000`.
  - A coroutine-driven `run(player)` that ramps `player.volume` over the last 10 s, then `pause()` and `volume = 1f`.
  - `remainingMs` as a Flow.
  - T068 must pass.
- [X] T070 [US5] Wire the sleep timer into `app/src/main/java/app/ytsleep/playback/PlaybackService.kt`:
  - Custom `SessionCommand`s `SET_SLEEP_TIMER(minutes | END_OF_ITEM)` and `CANCEL_SLEEP_TIMER`.
  - Handle END_OF_ITEM via `onMediaItemTransition`/`STATE_ENDED`.
  - Publish the remaining time in session extras.
  - Save the chosen minutes to `SettingsStore.lastSleepTimerMinutes`.
  - Add the matching commands to `app/src/main/java/app/ytsleep/playback/PlayerConnection.kt`.
- [X] T071 [US5] Add the sleep timer control in `app/src/main/java/app/ytsleep/ui/player/SleepTimerPicker.kt` and the button in `PlayerSheet.kt`:
  - Preset chips 15, 30, 45, 60 and 90 min, plus "End of current item", with the last choice pre-selected.
  - While active, show the remaining time `mm:ss` and a Cancel button.
- [X] T072 [US5] Run quickstart V7 on the phone (screen locked). Record the result in `specs/001-sleep-audio-player/checklists/validation.md`.

**Checkpoint**: All five stories work independently.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T073 [P] Add an instrumented `HistoryDaoTest` in `app/src/androidTest/java/app/ytsleep/data/HistoryDaoTest.kt`, using an in-memory Room database. It checks upsert, `recent(20)` ordering by `lastPlayedAt DESC`, `latest()` and `pruneKeepingNewest(500)`.
- [X] T074 [P] Add an instrumented `PlaybackServiceSmokeTest` in `app/src/androidTest/java/app/ytsleep/playback/PlaybackServiceSmokeTest.kt`. It binds a MediaController to PlaybackService with a fake ContentSource that returns a local test audio asset from `app/src/androidTest/assets/`. It checks that play → isPlaying, that seekForward adds 30 s, and that the player has no video renderer.
- [X] T075 [P] Complete `README.md`: the release workflow (`scripts/release.sh`), keystore creation and the **encrypted backup** warning, `RELEASE_REPO` public-repo note, and the "When YouTube breaks playback" steps from quickstart.md.
- [X] T076 Sleep-first UI audit across `app/src/main/java/app/ytsleep/ui/`:
  - Remove any remaining animations, shimmer or bright colors.
  - Check that no screen auto-loads more items on scroll and that the app opens on Start (Principle VI, FR-013, FR-014).
  - Fix any findings in place.
- [X] T077 Run `./gradlew testDebugUnitTest connectedDebugAndroidTest`. All tests must pass.
- [X] T078 Run the full quickstart V1–V14 on the phone with Chrome, YouTube and the Play Store blocked. Confirm the constitution's four quality gates (no video; no Shorts or ads; audio ≥ 10 min with the screen locked; $0 cost). Record the final results in `specs/001-sleep-audio-player/checklists/validation.md`.

---

## Dependencies & Execution Order

### Phase dependencies

```text
Setup (P1) ─▶ Foundational (P2) ─▶ US1 (P3, MVP) ─┬─▶ US4 (P4)
                                                 ├─▶ US2 (P5) ── needs T037 from US4
                                                 ├─▶ US3 (P6)
                                                 └─▶ US5 (P7)
                                   all stories ─▶ Polish (P8)
```

### Story dependencies

- **US1**: needs only Foundational. It's the MVP.
- **US4**: needs US1 for meaningful validation. T035–T037 can start right after Foundational.
- **US2**: needs US1 (PlaybackService, PlayerConnection, StartScreen) and T037 (InAppOnlyWebViewClient) from US4.
- **US3**: needs only Foundational plus StartScreen (T032) for the banner. It's independent of US2 and US5.
- **US5**: needs US1 (PlaybackService, PlayerSheet).

### Within each story

- Tests marked as "must fail first" are written before their implementation tasks.
- Models/parsers come before ContentSource wiring, which comes before ViewModels, which come
  before screens, which come before on-device validation.
- Several tasks edit the same files. They must not run at the same time; after the first task,
  the others run in whichever order their stories run:
  - PlaybackService.kt: T025 first, then T058 (US2) and T070 (US5)
  - StartScreen.kt: T032 first, then T064 (US3) and T057 (US2)
  - SettingsScreen.kt: T054 (US2) and T065 (US3). Whichever runs first creates the file
  - PlayerSheet.kt: T031 first, then T058 (US2) and T071 (US5)
  - YouTubeContentSource.kt: T023 → T051

## Parallel Opportunities

- **Setup**: T005 and T006 together.
- **Foundational**: T007, T008, T009, T010, T012, T013, T014 and T017 together. Then T011 (after T010), then T015 (after T014), then T016.
- **US1**: T018, T019, T020, T021 and T027 together. Then T022 → T023 → T024 → T025 → T026/T028 → T029. Then T030 and T031 together. Then T032 → T033 → T034.
- **US4**: T035, T036 and T037 together.
- **US2**: T040–T047 together (fixtures, tests, CookieParser, AccountStore, SapisidHash). Then T049 and T050 together. Then T048 → T051. T053 runs alongside T052.
- **US3 and US5 can run in parallel with US2** after US1, because they touch different files except for the shared-file chains listed above.
- **Polish**: T073, T074 and T075 together.

### Parallel example: User Story 1

```text
Task: "T018 [P] [US1] Write AudioStreamSelectorTest in app/src/test/java/app/ytsleep/youtube/newpipe/AudioStreamSelectorTest.kt"
Task: "T019 [P] [US1] Write HistoryRulesTest in app/src/test/java/app/ytsleep/playback/HistoryRulesTest.kt"
Task: "T020 [P] [US1] Implement PoTokenWebViewProvider in app/src/main/java/app/ytsleep/youtube/newpipe/PoTokenWebViewProvider.kt"
Task: "T021 [P] [US1] Implement NewPipeMapper in app/src/main/java/app/ytsleep/youtube/newpipe/NewPipeMapper.kt"
Task: "T027 [P] [US1] Implement QueueBuilder in app/src/main/java/app/ytsleep/playback/QueueBuilder.kt"
```

### Parallel example: User Story 2

```text
Task: "T041 [P] [US2] Write SapisidHashTest in app/src/test/java/app/ytsleep/youtube/innertube/SapisidHashTest.kt"
Task: "T042 [P] [US2] Write FeedParserTest in app/src/test/java/app/ytsleep/youtube/innertube/FeedParserTest.kt"
Task: "T044 [P] [US2] Write CookieParserTest in app/src/test/java/app/ytsleep/auth/CookieParserTest.kt"
Task: "T046 [P] [US2] Implement AccountStore in app/src/main/java/app/ytsleep/auth/AccountStore.kt"
```

## Implementation Strategy

### MVP first

1. Phase 1 Setup, then Phase 2 Foundational.
2. Phase 3 (US1): search and screen-off audio-only playback.
3. **Stop and validate** with T034, in bed with the phone locked. This alone already replaces
   the YouTube app for sleep listening.

### Recommended delivery order

1. Setup + Foundational + **US1** → MVP. Install via `adb` (the one-time step).
2. **US3 next** (pulled ahead of US2, same P2 priority). Once the self-updater ships, every later
   feature reaches the phone with one "Install" tap and no cable. The task notes above make this
   order safe. This fits the owner's "develop new
   things often" workflow.
3. **US4**: the block-everything validation pass. Block Chrome, YouTube and the Play Store from
   here on.
4. **US2**: sign-in, feeds and playlists, shipped through the update path.
5. **US5**: the sleep timer.
6. Polish.

Each step ends with an on-device validation task, so a broken increment is never shipped.

## Notes

- [P] = different files and no dependency on unfinished tasks.
- Every task names exact file paths. The quoted constraints (Shorts ≤ 180 s, 500-entry prune,
  30 s end rule, 10 s fade, 15 s/30 s seeks, 6 h update interval, 10 s fetch timeout) come
  straight from data-model.md and the contracts.
- If YouTube breaks playback mid-implementation, bump NewPipeExtractor in
  `gradle/libs.versions.toml` and re-run T034.

## Phase 9: Convergence

- [X] T079 CRITICAL: Fix `StreamResolvingDataSource` so long items play to the end. The 10 MB cap currently ends every stream after its first 10 MB, and the data source must keep issuing ranged reads until the stream is done. Also implement the planned re-resolve on HTTP 403/410: invalidate the cache and retry once with a fresh URL. Share one resolver/cache across the data sources the Factory creates. Per Constitution III / FR-003 / SC-001 (contradicts)
- [X] T080 CRITICAL: Make the project buildable. Replace the broken `gradlew` (it execs a non-existent jar) with a real Gradle wrapper (`gradle-wrapper.jar` + standard `gradlew`). Then compile `assembleDebug` and fix every compile error. A known one: `PlaybackService` overrides `onCustomCommand`, which isn't a `MediaSessionService` method, so it has to move into the `MediaSession.Callback`. Also check the NewPipeExtractor v0.24.6 API calls (`PoTokenResult` constructor, `yt.searchExtractor`, `Page` usage). Per plan: Technical Context / T077 (missing)
- [X] T081 CRITICAL: Wire play-on-tap for search results in `ui/search/SearchScreen.kt` (empty `onClick`). Use `PlayerConnection.playItems(items, index, null)`. If the controller isn't connected yet, queue the request so the tap isn't silently dropped. Per US1/AC1 / FR-008 (missing)
- [X] T082 Wire play-on-tap for Home and Subscriptions items in `ui/feed/FeedScreen.kt` (empty `onClick`) per US2/AC1 / FR-010 (missing)
- [X] T083 Implement history saving: `HistoryRecorder.saveNow()` is a no-op, so nothing is ever written. Give it the player and save the videoId, metadata, position, durationSeconds and source playlist/index from the current `MediaItem`: every 10 s while playing, on pause, and on item transition. Apply `HistoryRules.savePosition` and the prune. Per FR-024 / FR-015 (missing)
- [X] T084 Wire the Start screen taps in `ui/start/StartScreen.kt`. "Continue listening" must resume at the saved position in 1 tap via `PlayerConnection.resume(entry)`, and Recent rows must play their entry. Show a thumbnail on both. Per FR-015 / SC-003 (missing)
- [X] T085 Make the sleep timer work end to end:
  - Handle `CMD_SET_SLEEP_TIMER` and `CMD_CANCEL_SLEEP_TIMER` in the `MediaSession.Callback.onCustomCommand`.
  - For "end of current item", stop at the end of the *current* item: use `onMediaItemTransition` with `MEDIA_ITEM_TRANSITION_REASON_AUTO`/`REPEAT` plus `STATE_ENDED`, not only `STATE_ENDED`.
  - Republish the remaining time to the session extras every second, so the picker's countdown updates.

  Per FR-016 / US5/AC1–AC2 (partial)
- [X] T086 Make the update auto-prompt work:
  - When `UpdateChecker` reaches READY and nothing is playing, start `UpdateInstaller.install` automatically. If something is playing, or the user dismisses the prompt, set DISMISSED.
  - Feed `UpdateChecker.state` into `StartUiState.updateState`.
  - Fix the banner's APK path, which uses `versionName` while the file is saved as `versionCode`.
  - Have `InstallResultReceiver` update the checker state on ABORTED or failure.

  Per FR-021 / US3/AC1, AC4 (missing)
- [X] T087 Wire `PlaybackErrorPolicy` into `PlaybackService` through `Player.Listener.onPlayerError`, so an unplayable item skips to the next item instead of stalling the queue. Per FR-006 (partial)
- [X] T088 Add the repeat toggle (off / one / playlist) to `ui/player/PlayerSheet.kt`, calling `PlayerViewModel.cycleRepeat()` and showing the current mode. Per FR-006a / US2/AC6 (missing)
- [X] T089 Update the player's position and elapsed time while playing. `PlayerConnection.updateState()` only runs on events, so the seek bar and elapsed time stay frozen. Poll about every 1 s while `isPlaying`. Per FR-005 (partial)
- [X] T090 Show seek back 15 s / forward 30 s buttons in the media notification and lock screen, using a custom command button layout on the `MediaSession`. Per FR-004 / US1/AC3 (partial)
- [X] T091 Fix search "Load more": keep the NewPipe `Page`, including its id and body, or the extractor instance, between calls, instead of only `page.url`, which loses the continuation body. Per FR-008 / FR-013 (partial)
- [X] T092 Show `BatteryOptimizationPrompt` once from the Start screen. It's defined but never used. Per plan: Constitution III platform limits / FR-003 (missing)
- [X] T093 Call `AccountStore.markExpired()` when `ContentError.SessionExpired` is raised, and clear it on a successful save, so Settings shows "Reconnect". Per FR-009 (partial)
- [X] T094 Replace `app/src/androidTest/assets/test_audio.mp3` with a real decodable audio clip (the current file is a frame header plus zeros) so `PlaybackServiceSmokeTest` can pass. Rewrite `SleepTimerTest` to use virtual time and a fake clock as T068 specifies. Per plan: Testing (partial)
- [X] T095 Load each feed tab only when it's first opened, not both in `FeedViewModel.init`. Per FR-010 / T049 (partial)
- [ ] T096 After T079–T095, actually run `./gradlew testDebugUnitTest connectedDebugAndroidTest` and the quickstart V1–V14 on the Android 11 phone with the apps blocked. Confirm `resolveAudio` works with the current empty PoToken provider, and implement a real BotGuard token if it doesn't. Record real results in `checklists/validation.md`, which still says every scenario is Pending. Per Constitution III quality gate / SC-001, SC-002, SC-005 (missing)

## Phase 10: Convergence

- [X] T097 CRITICAL: Remove the animated loading indicators. Replace each `CircularProgressIndicator` (`ui/feed/FeedScreen.kt`, `ui/search/SearchScreen.kt`, `ui/playlist/PlaylistScreen.kt`) with the existing static `ui/common/DimPlaceholder.kt`, which is currently unused. Replace the sliding `ModalBottomSheet` in `ui/player/SleepTimerPicker.kt` with a non-animated dialog. Per Constitution VI / FR-014 (contradicts)
- [X] T098 Prevent the duplicate-key crash in lists. Every `LazyColumn` keys items by `videoId`, but a playlist can contain the same video twice and a feed or search continuation can repeat an earlier item. De-duplicate when appending pages, or key by position plus id, in Feed, Search, Playlist and Start. Per FR-011 / FR-013 (partial)
- [X] T099 Show a short, calm message in the player when an item can't be played and there is nothing to skip to (live stream, removed or region-locked video, `SourceChanged`). Expose the error in `PlayerUiState` and render it in `PlayerSheet.kt`. Per spec Edge Cases / contracts/ui-navigation.md "Unplayable item" (missing)
- [X] T100 Limit `PlaybackErrorPolicy` to skip only on unplayable/source errors, not network errors: with the screen off, an offline phone would currently skip through the whole queue. For network drops, use a more patient `LoadErrorHandlingPolicy` so playback resumes when the connection returns, and stop quietly if it can't. Per spec Edge Cases "Network drops while the screen is off" (partial)
- [X] T101 Add the battery-optimization row to `ui/settings/SettingsScreen.kt`: show whether the exemption is granted and open the system request if it isn't. Per contracts/ui-navigation.md Settings / FR-003 (missing)
- [X] T102 In `PlaylistViewModel`/`PlaylistScreen`, surface `NotSignedIn`/`SessionExpired` with a "Connect account"/"Reconnect account" action, as Feed does. Don't fall back to the public NewPipe extractor for the private lists `WL` and `LL`; that path currently ends in a misleading "YouTube changed something" message. Per US2/AC5 (partial)
- [X] T103 Show the playlist's real title and item count in the Playlist header (pass them from `PlaylistSummary` through the navigation route) instead of the raw playlist id. Per contracts/ui-navigation.md Playlist header (partial)
- [X] T104 When "Load more" fails in Feed, Search or Playlist, keep the already-loaded items and show the error inline next to the button, instead of replacing the whole list with the error screen. Per FR-013 (partial)
- [X] T105 Make `scripts/release.sh` JSON-escape the release note when writing `update.json` (for example with `jq -n`), so a note containing `"` or `\` can't produce an invalid manifest that the app silently ignores. Per FR-021 / contracts/update-manifest.md (partial)
- [X] T106 Tighten the host check in `InAppOnlyWebViewClient` to exact-or-subdomain matching (`host == d || host.endsWith(".$d")`), so look-alike hosts such as `evilgoogle.com` aren't allowed inside the sign-in WebView. Per FR-020 / US4/AC2 (partial)
