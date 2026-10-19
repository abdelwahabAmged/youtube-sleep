# Quickstart & Validation: Sleep Audio Player for YouTube

How to build the app, install it once, ship updates, and prove that the feature meets the spec
and the constitution's quality gates.

## Prerequisites (dev machine)

- JDK 17, Android SDK (platform 36, build-tools), and the Android platform-tools (`adb`).
- GitHub CLI `gh`, signed in. It needs access to `RELEASE_REPO`, which must be a public repo
  (research R6).
- A release keystore, created once:

  ```bash
  keytool -genkeypair -v -keystore ~/keys/ytsleep-release.jks -alias ytsleep -keyalg RSA -keysize 4096 -validity 36500
  ```

  Put its passwords in `~/.gradle/gradle.properties` (`YTSLEEP_STORE_PASSWORD`, etc.), never in
  the repo.

> ⚠️ **Back up the keystore** (for example as an encrypted copy in your password manager). If you
> lose it, you can no longer update the app. You'd have to uninstall it and lose your history.

## Phone setup (one time)

1. The phone runs Android 11. Every update will need one "Install" confirmation, which the app
   opens by itself (research R6, R11).
2. Turn on Developer options → USB debugging (or Wireless debugging).
3. Run `./gradlew assembleRelease && adb install app/build/outputs/apk/release/app-release.apk`.
4. Open the app. Allow "ignore battery optimization" when it asks, then allow "Install unknown
   apps" for YT Sleep when the app asks (Settings → Apps → Special access → Install unknown
   apps). Both are one-time.
5. Settings → Account → Connect, then sign in inside the app (research R4). If Google refuses,
   use Settings → Account → Paste cookies with cookies exported from a desktop browser.
6. You can now block Chrome, YouTube and the Play Store in AppBlock. You won't need them again.

## Run the automated tests

```bash
./gradlew testDebugUnitTest          # Shorts/ad filter, InnerTube parsers, manifest, timer, history rules
./gradlew connectedDebugAndroidTest  # Room DAO + service smoke test (phone connected)
```

Expected: all tests pass. The Shorts/ad filter tests are required by the constitution.

## Publish an update

```bash
RELEASE_REPO=owner/youtube-sleep-releases ./scripts/release.sh "One-line release note"
```

Expected:
- A new GitHub Release `vX.Y.Z` with `youtube-sleep-X.Y.Z.apk` and an `update.json` that matches
  [contracts/update-manifest.md](./contracts/update-manifest.md).
- On the phone, the next app launch (with nothing playing) opens the system install prompt by
  itself. One tap on "Install" updates the app.

## Validation scenarios (on the phone)

Run these with **Chrome, the YouTube app and the Play Store blocked in AppBlock** (Story 4).

| # | Scenario | Steps | Expected | Covers |
|---|---|---|---|---|
| V1 | Audio only, screen off | Search "rain sounds 10 hours", tap a result, lock the phone, wait 10 min | Audio continuous. Only static thumbnail and controls ever shown. No ad heard | Story 1, FR-001–003, gate 1 & 3 |
| V2 | Lock-screen controls | While locked: pause, play, −15 s, +30 s | Each works. Notification shows title and thumbnail | FR-004 |
| V3 | No Shorts | Search "funny cats", check results. Open Home, Subscriptions and a playlist that contains a Short | No Short and no item ≤ 3:00 appears anywhere | FR-012, SC-002, gate 2 |
| V4 | No ads | Play 5 videos known to show ads on YouTube | No ad audio or visuals. No sponsored items in feeds | FR-002, SC-002 |
| V5 | Feed tabs and paging | Open Feed → Home, then Subscriptions. Scroll to the end | Items load once. More load only after tapping "Load more" | FR-010, FR-013 |
| V6 | Playlist autoplay and repeat | Play a playlist, lock. Then test repeat one and repeat playlist | It advances to the next item. Stops after the last when repeat is off. Loops correctly when repeat is on | FR-006, FR-006a |
| V7 | Sleep timer | Set 15 min, lock | Audio fades out and stops at about 15 min | FR-016 |
| V8 | Interruption | Get a phone call during playback | Pauses. Resume from the lock screen works | FR-007 |
| V9 | Resume | Kill the app mid-item, reopen | "Continue listening" resumes within ±10 s in 1 tap | FR-015, FR-024, SC-003 |
| V10 | Long item and URL expiry | Play a 10 h item overnight (or set the device clock forward 7 h in a debug build) | Plays to the end or until the timer. No stop at about 6 h | SC-001, research R5 |
| V11 | Update | Publish a change that shows the version in About. Open the app with nothing playing, tap "Install" on the prompt that appears. Repeat while audio is playing, then dismiss the prompt once | The prompt appears by itself and 1 tap installs. While playing, there's no prompt until playback stops. After dismissing, the "Update ready" banner re-opens it. The new version runs, with sign-in, history and repeat mode kept | Story 3, FR-021–023, SC-004 |
| V12 | Offline update check | Airplane mode, open the app | Opens normally with no error | Story 3, scenario 3 |
| V13 | No external hand-off | Tap every link-like element (channel names, thumbnails) | Nothing opens outside the app | FR-020 |
| V14 | Cost | Review services used | Only GitHub (free) and YouTube. $0 | FR-026, SC-006, gate 4 |

**Done** means V1–V14 pass and the automated tests are green. Those cover the constitution's
four quality gates: no video, no Shorts or ads, 10+ minutes of screen-off audio, and $0 cost.

## When YouTube breaks playback

1. Run `./gradlew dependencyUpdates` (or check the NewPipeExtractor releases page).
2. Bump the extractor version. If InnerTube parsing broke, refresh the fixtures and fix the
   parser.
3. Run `./scripts/release.sh "Fix YouTube playback"`. The phone gets it on its next launch.
