# Contract: Update Manifest (`update.json`)

This is the interface between the dev machine (publisher) and the app on the phone (consumer).
It is the only external interface the project owns. See research R6.

## Location

Each GitHub Release carries an `update.json` asset next to the APK. The app always fetches:

```
GET https://github.com/{RELEASE_REPO}/releases/latest/download/update.json
```

- `RELEASE_REPO` (e.g. `owner/youtube-sleep-releases`) is compiled into the app as a build
  setting.
- The request needs no authentication, so the repo must be public.

## Schema (version 1)

```json
{
  "schema": 1,
  "versionCode": 7,
  "versionName": "0.4.0",
  "apkUrl": "https://github.com/owner/youtube-sleep-releases/releases/download/v0.4.0/youtube-sleep-0.4.0.apk",
  "sha256": "9f2c…64 hex chars…",
  "sizeBytes": 8421337,
  "minSdk": 29,
  "publishedAt": "2026-10-19T21:30:00Z",
  "notes": "Sleep timer fade is now 10 s."
}
```

| Field | Required | Rule |
|---|---|---|
| schema | yes | Must be `1`. If it's unknown, the app ignores the manifest and opens normally |
| versionCode | yes | A positive integer that goes up with every release. It must equal the APK's `versionCode` |
| versionName | yes | Shown in the update banner and in Settings → About |
| apkUrl | yes | HTTPS. The host must be `github.com` or `objects.githubusercontent.com` (after redirects) |
| sha256 | yes | Lowercase hex digest of the APK file. If the download doesn't match, it's discarded |
| sizeBytes | yes | Used for a sanity check and for progress |
| minSdk | yes | If it's higher than the device's SDK, the update isn't offered |
| publishedAt | no | ISO-8601 |
| notes | no | One short plain-text line, shown under the banner |

## App behaviour

| Situation | Behaviour |
|---|---|
| Fetch fails (offline, 404, timeout of 10 s) | Do nothing silently and open normally (Story 3, scenario 3) |
| `versionCode` ≤ the installed version | No banner |
| Newer version | Download the APK in the background and check its SHA-256 |
| APK verified, app in foreground, **nothing playing** | Commit the `PackageInstaller` session automatically and launch the system confirmation on `STATUS_PENDING_USER_ACTION`. The user's only action is "Install" (always required on Android 11) |
| APK verified but audio playing | Hold back the prompt. Show it when playback stops, or on the next launch |
| Prompt dismissed by the user | Show an "Update ready — {versionName}" banner on the start screen. Tapping it re-opens the prompt. Don't auto-prompt again for the same `versionCode` until the next launch |
| "Install unknown apps" not granted | Open the system settings page for this app instead of the prompt (normally granted once during setup) |
| Install fails | Show the banner with "Retry". The current version keeps working |

## Publisher rules (`scripts/release.sh`)

1. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`.
2. Run `./gradlew assembleRelease`, signed with the permanent release keystore.
3. Compute the SHA-256 and size, then write `update.json`.
4. Run `gh release create v{versionName} {apk} update.json --repo {RELEASE_REPO} --notes "{notes}"`.

   The release must be marked "latest", which is the default.
