# Research: Sleep Audio Player for YouTube

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-10-19

Each decision lists what was chosen, why, and what was rejected. Items marked **Risk** must be
re-checked when implementation starts, because YouTube changes often.

---

## R1. App delivery technology (how the app lives on the phone)

**Decision**: A native Android app (APK), written in Kotlin and installed directly on the phone
(sideloaded). It is not a PWA and not a browser wrapper.

**Rationale**:
- **AppBlock independence (FR-019, Story 4)**: an installed PWA on Android is a WebAPK that runs
  inside Chrome. If Chrome is blocked, the PWA is either blocked too or behaves unpredictably. A
  native APK is its own package, so it only depends on Android system components (the system
  WebView and the package installer), which app blockers don't treat as user apps.
- **Content access**: a web page can't fetch YouTube streams directly (browser cross-origin
  restrictions). A PWA would need a proxy server, which breaks Principle IV (zero cost) and
  adds an always-on component. A native app can talk to YouTube directly.
- **Screen-off playback (FR-003)**: a native foreground media service is the most reliable way to
  keep audio running for 10+ hours with the screen off on Android.

**Alternatives considered**:
- *PWA*: rejected because it depends on Chrome and needs a proxy server (see above).
- *Flutter / React Native / Capacitor*: these would still need native code for the media service
  and stream extraction, and Dart/JS extractors are less battle-tested. Two layers is more
  complex than one (Principle V).
- *Fork LibreTube / NewPipe / Tubular*: these are large video-first apps with no Google account
  login. Stripping out video would cost more than a small focused app (Principles I, V).
- *Fork Metrolist / InnerTune*: these target YouTube Music, not regular YouTube feeds and
  playlists. We borrow their login approach instead (R4).

---

## R2. Getting playable audio, search results and public playlist data from YouTube

**Decision**: Use **NewPipeExtractor** (TeamNewPipe, GPLv3, pure Java library) for search, stream
resolution (audio-only formats) and the YouTube integrity tokens (PO tokens) it needs. Pin the
latest release at implementation time and upgrade it whenever YouTube breaks playback.

**Rationale**:
- It is the most actively maintained open-source YouTube extractor on the JVM. For example, a
  playback breakage in January 2026 was fixed within days (NewPipe 0.28.2).
- It returns separate audio-only stream formats (e.g. Opus/WebM, AAC/M4A), so the app never
  downloads or renders video (Principle I).
- The streams it returns contain no ads. YouTube ads are separate requests made by YouTube's own
  player, and we never load that player (Principle II).
- Search result items expose an `isShortFormContent()` flag and a duration, which the Shorts rule
  needs (FR-012).
- No API key and no quota, so it's free (Principle IV).

**Licensing**: GPLv3 is fine because the app is personal and not distributed. If the source repo is
public, the app source must also be GPLv3, which is acceptable.

**Risk**: YouTube regularly changes signature/throttling ciphers and PO token rules. Mitigation:
- The extractor is hidden behind a `ContentSource` interface
  ([contracts/content-source.md](./contracts/content-source.md)).
- Playback failures show a clear error (spec Edge Cases).
- A fix ships as a dependency bump plus a release through the one-tap update path (R6).

**Alternatives considered**:
- *YouTube Data API v3*: has no stream URLs and no Home feed, and has a quota. Rejected as the
  main source.
- *Calling YouTube's internal API (InnerTube) ourselves for playback*: we would have to
  maintain cipher and PO-token handling ourselves. Rejected; we use InnerTube only for the
  signed-in browsing in R3.
- *yt-dlp on a server*: costs money, needs an always-on server, and may be blocked from
  datacenter IPs. Rejected.

---

## R3. Signed-in browsing: Home feed, Subscriptions, the user's playlists

**Decision**: A small in-app **InnerTube client** (YouTube's internal web API, the same one
youtube.com uses) that sends the user's session cookies. It calls these endpoints:

| Need | InnerTube `browse` id |
|---|---|
| Home feed (FR-010) | `FEwhat_to_watch` |
| Subscriptions feed (FR-010) | `FEsubscriptions` |
| The user's playlists (FR-011) | `FEplaylist_aggregation` (fallback: `FElibrary`) |
| Playlist items (FR-011) | `VL<playlistId>`, including `VLWL` (Watch later) and `VLLL` (Liked videos) |
| More items (FR-013) | the `continuation` token from the previous response |

Requests use the WEB client context, a `Cookie` header and an `Authorization: SAPISIDHASH`
header computed from the `SAPISID` cookie. This is the documented community approach used by
Metrolist, InnerTune and ytmusicapi.

**Rationale**: this is the only free way to get the personalized Home feed. The official Data
API has no Home feed. NewPipeExtractor doesn't support accounts.

**Parsing and filtering**: the response parser maps renderers to `MediaItem`s:
- Keeps: `richItemRenderer` → `videoRenderer`/`lockupViewModel`, `playlistVideoRenderer`, and
  the playlist grid lockups.
- Drops: `adSlotRenderer`, `promotedSparklesWebRenderer`, `statementBannerRenderer` (ads, FR-002).
- Marks as Shorts (then hidden by the Shorts filter): `reelItemRenderer`,
  `shortsLockupViewModel`, `richSectionRenderer` that contains a reel shelf, and any
  `/shorts/` URL (FR-012).
- Parsers are tested against saved JSON fixtures.

**Risk**: the response formats change from time to time. Mitigation: the parsers are small,
covered by fixture tests, and lenient (an unknown renderer is skipped, not a crash). A fix
ships through the update path.

**Alternatives considered**:
- *Data API v3 with OAuth*: covers subscriptions and playlists but not Home. OAuth on a phone
  also needs a browser (Custom Tabs → Chrome, which is blocked).
- *TV-client OAuth device flow (SmartTube-style)*: Google has restricted it for third-party
  use since late 2024. Too fragile.

---

## R4. Sign-in without a browser (FR-009, FR-019, Story 4)

**Decision**: An **in-app Android WebView** opens the Google sign-in page with the youtube.com
return address
(`https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/`).
After the WebView lands on youtube.com signed in, the app reads the cookies from
`CookieManager`, keeps the ones it needs (`SAPISID`, `__Secure-3PAPISID`, `SID`, `HSID`, `SSID`,
`APISID`, `LOGIN_INFO`, …) and stores them in app-private storage.

**Rationale**:
- The WebView is a system component, not Chrome-the-app, so it works with Chrome blocked.
- It's a one-time step; cookies last months.
- When they expire, the app shows a "Reconnect account" prompt (spec Edge Cases).
- Metrolist and InnerTune ship this exact flow.

**Fallback** (allowed because the spec permits a one-time step with another device): a
"Paste cookies" screen. The user exports youtube.com cookies on a desktop browser and pastes
them in. This covers the case where Google refuses sign-in in the embedded WebView
("This browser or app may not be secure").

**Security**:
- Cookies are only sent to `*.youtube.com` / `*.google.com`.
- They are stored in app-private storage (no backup: `android:allowBackup="false"`) and never
  logged.
- "Sign out" in Settings deletes them and clears WebView cookies.

---

## R5. Background, screen-off playback and lock-screen controls

**Decision**: **AndroidX Media3**:
- `ExoPlayer` running inside a `MediaSessionService` (foreground service of type
  `mediaPlayback`).
- The player is audio-only: only audio formats are requested, and there is no video renderer
  or surface. `MediaSession` drives the lock-screen and notification controls.

Details:
- **Stream expiry**: YouTube stream URLs expire after about 6 hours, and 10-hour items must keep
  playing (SC-001). Queue items therefore use a placeholder URI `ytsleep://<videoId>`. A
  `ResolvingDataSource` resolves it to a fresh audio URL when a read opens. On an HTTP 403/410 it
  re-resolves once and continues from the same byte position.
- **YouTube throttling**: YouTube throttles single large requests, so reads use ranged requests of
  up to about 10 MB each (the same approach as NewPipe's YoutubeHttpDataSource).
- **Repeat**: Media3 `REPEAT_MODE_OFF/ONE/ALL` maps directly to off / repeat one / repeat
  playlist (FR-006a).
- **Interruptions**: `handleAudioFocus = true` pauses on calls and alarms (FR-007). Unplugging
  headphones also pauses playback.
- **Seek buttons**: the notification and lock screen show previous, seek back, play/pause, seek
  forward and next. Seek steps are 30 s forward and 15 s back (FR-004).
- **Battery**: the app asks once to be excluded from battery optimization (spec Assumption).
  On Android 14+ it declares `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. Media notifications don't
  need the `POST_NOTIFICATIONS` permission.
- **Sleep timer (FR-016)**: a coroutine in the service. In the last 10 s it lowers the player
  volume linearly to 0, then pauses and restores the volume. "End of current item" sets a flag
  that pauses at the next item change.

**Alternatives considered**: a WebView with YouTube's own player hidden. Rejected: it shows ads,
and it violates Principle I's "zero visible area" fragility.

---

## R6. Getting new versions onto the phone (FR-021, FR-022, SC-004, Story 3)

**Decision**: An **in-app self-updater backed by GitHub Releases**.

1. **Dev machine**: `scripts/release.sh`
   - bumps `versionCode` / `versionName`,
   - builds a signed release APK,
   - writes `update.json` ([contracts/update-manifest.md](./contracts/update-manifest.md)),
   - runs `gh release create vX.Y.Z app-release.apk update.json`.
   A GitHub Actions workflow doing the same on tag push is optional.
2. **Phone, on every app launch** (and at most every 6 h while open):
   - fetch `https://github.com/<owner>/<repo>/releases/latest/download/update.json`;
   - if `versionCode` is greater than the installed version, download the APK in the background
     and check its SHA-256.
3. **The install prompt opens by itself; one tap installs** (the user's phone is on
   **Android 11**):
   - Once the APK is verified and the app is in the foreground with nothing playing, the app
     commits a `PackageInstaller` session on its own.
   - Android 11 always answers a self-update with `STATUS_PENDING_USER_ACTION`. The app
     immediately launches the returned system confirmation, so the user's **only action is
     tapping "Install"** (FR-021, SC-004). No in-app tap comes first.
   - If audio is playing, the prompt is held back until playback stops or the next launch, so
     it never interrupts sleep.
   - If the user dismisses the prompt, an "Update ready" banner on the start screen re-opens it
     with one tap.
   - **Permission**: "Install unknown apps" for this app is granted once during initial setup
     (quickstart). If it was ever revoked, the app opens that system settings page instead of
     the prompt.
   - **Newer phones**: on Android 12+ the session also sets `USER_ACTION_NOT_REQUIRED`. Once the
     app is its own installer of record, those updates install silently. This is a future bonus;
     the design doesn't depend on it.
4. **Data survives updates** (FR-022): updates install over the existing app with the same
   package name and signing key, so Android keeps all app data.

**Signing key**: this is critical. Every release must use the same keystore. Losing it means
uninstalling and losing local data. The keystore lives outside the repo, with an encrypted
backup documented in quickstart.

**Repo visibility**: unauthenticated release downloads need a public repo. If the source repo
stays private, publish releases to a separate public `youtube-sleep-releases` repo. The script
takes `RELEASE_REPO` as a setting.

**Developer fast path** (optional, not the main update path): with Android wireless debugging
paired, `./gradlew installRelease` pushes straight from the dev machine over Wi-Fi.

**Alternatives considered**:
- *Obtainium / F-Droid*: an extra third-party app the user may later block. Rejected as the main
  path.
- *Google Play internal testing*: $25 fee (Principle IV) and needs the Play Store (may be
  blocked).
- *Firebase App Distribution*: needs the Firebase tester app or a browser.
- *Over-the-air JS bundles (Expo Updates / Capgo)*: only possible with a JS app (rejected in R1),
  and they need a server.

---

## R7. First install (one-time step)

**Decision**: Install the first APK from the dev machine with `adb install` (USB or wireless
debugging). Alternatively, use any file manager or a one-time browser download before turning
the block on. Everything after that goes through R6, so blocked apps are never needed again
(FR-019).

---

## R8. Local data storage (FR-024, FR-025, FR-006a)

**Decision**:
- **Room (SQLite)** for listening history: resume positions and recently played items.
- **Jetpack DataStore** for settings: repeat mode, last sleep-timer choice, Shorts threshold
  (fixed at 180 s), last update check.
- Account cookies are a separate DataStore file, excluded from backups.

**Rationale**: these are standard, offline and free. History could grow to thousands of rows,
which needs indexed queries ("recent 20").

**Alternatives**: a single JSON file (rejected: awkward partial updates while playback writes
positions every 10 s).

---

## R9. UI stack and theme (FR-014, Principle VI)

**Decision**:
- **Jetpack Compose** (Material 3) with a fixed dark theme: near-black background (#0B0B0F), dim
  gray text, no dynamic color, no motion beyond default fades (animations shortened or disabled).
- **Coil** for thumbnails.
- **Navigation Compose** for screens.
- No DI framework; a small manual `AppContainer` (Principle V).

The app opens on the start screen ([contracts/ui-navigation.md](./contracts/ui-navigation.md)),
never on a feed.

---

## R10. Testing strategy

**Decision**:
- **JVM unit tests** (JUnit 4 + kotlinx-coroutines-test) for:
  - the Shorts/ad filter (FR-012, FR-002), required by the constitution;
  - InnerTube parsers against saved JSON fixtures;
  - update manifest parsing and version comparison;
  - the sleep-timer state logic;
  - history/resume rules.
- **A few instrumented tests** (Room DAO, PlaybackService startup).
- **Manual on-device checks** from [quickstart.md](./quickstart.md) cover background playback,
  lock-screen controls and AppBlock independence, as the constitution's quality gates require.

---

## R11. Target platform details

- **minSdk 29** (Android 10, spec Assumption), **targetSdk 36**, Kotlin 2.x, Java 17 toolchain,
  Gradle Kotlin DSL with a version catalog.
- **The owner's phone runs Android 11** (confirmed 2026-10-19). Silent self-update
  (`USER_ACTION_NOT_REQUIRED`) needs Android 12+ (API 31), so on this phone every update shows
  the system "Install" confirmation.
- R6 therefore makes that confirmation the *only* user action: the app downloads the update
  first and opens the prompt by itself, which satisfies constitution §III ("at most via a single
  simple user action").
- The documented platform limitation is that the action can't be zero on Android 11.
