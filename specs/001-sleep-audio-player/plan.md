# Implementation Plan: Sleep Audio Player for YouTube

**Branch**: `001-sleep-audio-player` | **Date**: 2026-10-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/001-sleep-audio-player/spec.md`

## Summary

A personal Android app that plays YouTube content as **audio only**, with no Shorts and no ads.
It keeps playing with the screen off, opens on a calm "Continue listening" start screen, and
lets the user search, browse the Home and Subscriptions feeds, and play their own playlists.

**Technical approach**:
- **App**: a small native Kotlin/Compose app, sideloaded as an APK so that it doesn't depend
  on Chrome, YouTube or the Play Store.
- **Audio**: NewPipeExtractor resolves audio-only streams. Media3 ExoPlayer plays them in a
  foreground `MediaSessionService`.
- **Account**: a minimal InnerTube client uses session cookies from a one-time in-app WebView
  sign-in to fetch the personalized Home, Subscriptions and playlists.
- **Updates**: an in-app self-updater for new APKs published to GitHub Releases. It downloads
  in the background and opens Android's install prompt by itself (only while nothing is
  playing), so the only action on the phone is tapping "Install".

## Technical Context

**Language/Version**: Kotlin 2.x on JDK 17 toolchain; Android minSdk 29 (Android 10), targetSdk 36

**Primary Dependencies**:
- AndroidX Media3 (ExoPlayer, MediaSession)
- NewPipeExtractor (YouTube search and audio stream resolution)
- Jetpack Compose (Material 3) and Navigation Compose
- OkHttp, kotlinx.serialization (InnerTube JSON), Coil (thumbnails)
- Room, DataStore
- Android `PackageInstaller` (self-update)

**Storage**:
- Room/SQLite: listening history and resume positions.
- DataStore: settings, and account cookies (cookies are excluded from backup).
- All storage is on the device only.

**Testing**:
- JUnit 4 and kotlinx-coroutines-test for JVM unit tests: filters, parsers with JSON fixtures,
  update manifest, timer, history rules.
- AndroidX instrumented tests: Room and a service smoke test.
- Manual on-device scenarios from [quickstart.md](./quickstart.md).

**Target Platform**: The owner's Android phone, running **Android 11** (the app supports Android 10+). It runs with the browser, YouTube and the Play Store blocked by AppBlock.

**Project Type**: Mobile app (single-module native Android). There is no backend.

**Performance Goals**:
- Audio starts within about 3 s of a tap on a normal connection.
- The start screen is usable within about 1 s of launch.
- Continuous playback of 10+ hours with the screen off (SC-001).

**Constraints**:
- No video rendering at all.
- No Shorts or ads in any list.
- $0 running cost.
- No dependency on blocked apps after the first install.
- Dark, low-motion UI.
- Stream URLs expire after about 6 h, so they must be re-resolved transparently.

**Scale/Scope**:
- 1 user, 1 device.
- About 7 screens (Start, Feed, Search, Playlist, Player sheet, Settings, Connect account).
- History of up to 500 entries.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | How the design complies | Status |
|---|---|---|
| I. Audio-Only Playback | `resolveAudio` returns audio-only formats. ExoPlayer has no video renderer or surface. The UI shows only static thumbnails and controls. YouTube's player is never embedded | ✅ |
| II. No Shorts, No Ads | The Shorts rule (marked, or ≤180 s) and ad-renderer dropping both live in `ContentSource` and are covered by unit tests. Extracted streams contain no ads. The app adds no promotions | ✅ |
| III. Background & Screen-Off (Mobile-First) | Media3 `MediaSessionService` foreground service with lock-screen controls. Installable APK with its own icon. Updates: the self-updater downloads in the background and opens the system install prompt by itself, so the user's single action is "Install". Platform limits are documented: Android 11 can't install silently, so one confirmation per update is the minimum; "Install unknown apps" and the battery exemption are granted once during setup | ✅ |
| IV. Zero-Cost | GitHub Releases (free), NewPipeExtractor (free, no quota) and InnerTube (no quota). No servers, no Play fee | ✅ |
| V. Personal Scope & Simplicity | Single user, single Gradle module, no DI framework, no backend. Content is used only for personal playback | ✅ |
| VI. Sleep-First | Fixed dark theme, no shimmer or animations. Opens on the start screen, not a feed. Feeds are paged with "Load more" (no infinite scroll). No autoplay of unrelated items. No notifications besides playback | ✅ |
| Platform: device/OS stated | Android 11, confirmed (spec Assumptions) | ✅ |
| Updates: dev publish + ≤1 phone action | `scripts/release.sh` on the dev machine, then one "Install" tap on the auto-opened system prompt | ✅ |
| Content source fragility documented | research R2/R3: risks and the fix-and-release mitigation | ✅ |
| Privacy | No analytics or tracking. Cookies stay local, are never logged and are excluded from backup | ✅ |

**Post-design re-check (after Phase 1)**: still passes. The contracts add no servers, no video
surfaces and no engagement features. The analysis finding about the update path is resolved:
- On Android 11, silent installs aren't possible (they need API 31+).
- So the app removes every other step: it downloads first and opens the system prompt
  automatically, leaving exactly one action.
- The "Install unknown apps" permission moves into one-time setup.
- On Android 12+, updates would become silent once the app is its own installer of record.

## Project Structure

### Documentation (this feature)

```text
specs/001-sleep-audio-player/
├── plan.md              # This file
├── research.md          # Phase 0 decisions (R1–R11)
├── data-model.md        # Entities, rules, playback state machine
├── quickstart.md        # Build, install, release, on-device validation (V1–V14)
├── contracts/
│   ├── content-source.md   # Internal YouTube boundary + guarantees
│   ├── update-manifest.md  # update.json published per release
│   └── ui-navigation.md    # Screens, tap budgets, empty/error states
├── checklists/requirements.md
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
app/
├── build.gradle.kts                    # versionCode/versionName, signing from ~/.gradle props, RELEASE_REPO buildConfigField
└── src/
    ├── main/
    │   ├── AndroidManifest.xml         # FOREGROUND_SERVICE(_MEDIA_PLAYBACK), REQUEST_INSTALL_PACKAGES,
    │   │                               # UPDATE_PACKAGES_WITHOUT_USER_ACTION, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, allowBackup=false
    │   └── java/app/ytsleep/
    │       ├── YtSleepApp.kt           # AppContainer (manual wiring)
    │       ├── MainActivity.kt
    │       ├── ui/
    │       │   ├── theme/              # dark sleep theme
    │       │   ├── start/              # Continue listening, Recent, My playlists, update banner
    │       │   ├── feed/               # Home | Subscriptions tabs
    │       │   ├── search/
    │       │   ├── playlist/
    │       │   ├── player/             # player sheet, repeat toggle, sleep timer picker
    │       │   ├── settings/           # account, battery, about
    │       │   └── account/            # WebView sign-in, paste-cookies
    │       ├── youtube/
    │       │   ├── ContentSource.kt    # interface + ContentError (contracts/content-source.md)
    │       │   ├── YouTubeContentSource.kt
    │       │   ├── ContentFilter.kt    # Shorts rule + ad drop (FR-002, FR-012)
    │       │   ├── newpipe/            # NewPipeExtractor wiring, Downloader impl, audio stream selection
    │       │   └── innertube/          # client, SAPISIDHASH auth, browse parsers
    │       ├── auth/                   # AccountStore (cookies), session status
    │       ├── playback/
    │       │   ├── PlaybackService.kt  # MediaSessionService + ExoPlayer (audio only)
    │       │   ├── StreamResolvingDataSource.kt  # ytsleep://id → fresh URL, re-resolve on 403/410, ranged reads
    │       │   ├── QueueBuilder.kt
    │       │   ├── SleepTimer.kt
    │       │   └── HistoryRecorder.kt
    │       ├── data/
    │       │   ├── db/                 # AppDatabase, HistoryEntry, HistoryDao
    │       │   └── SettingsStore.kt
    │       └── update/
    │           ├── UpdateManifest.kt
    │           ├── UpdateChecker.kt
    │           └── UpdateInstaller.kt  # PackageInstaller session
    ├── test/java/app/ytsleep/          # JVM unit tests
    │   ├── youtube/ContentFilterTest.kt
    │   ├── youtube/innertube/*ParserTest.kt
    │   ├── update/UpdateManifestTest.kt
    │   ├── playback/SleepTimerTest.kt
    │   └── data/HistoryRulesTest.kt
    ├── test/resources/innertube/       # recorded JSON fixtures (home, subscriptions, library, playlist, with shorts & ads)
    └── androidTest/java/app/ytsleep/   # HistoryDaoTest, PlaybackServiceSmokeTest
scripts/
└── release.sh                          # bump version → assembleRelease → update.json → gh release create
```

**Structure decision**: a single Android application module. The YouTube-facing code is
isolated under `youtube/` behind `ContentSource`, so fixes for YouTube changes stay local.
There is no backend, which keeps Principles IV and V. A separate pure-Kotlin module for the
filters and parsers was considered, but it isn't needed: JVM unit tests run fine in
`app/src/test`.

## Complexity Tracking

No constitution violations. The table is intentionally empty.
