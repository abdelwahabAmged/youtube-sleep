# YT Sleep

A personal Android app that plays YouTube audio only — no video, no Shorts, no ads. Works
with the screen off. Built for Android 11.

## Dev machine setup (one time)

You need JDK 17 and the Android SDK (platform 36, build-tools 35). Without Android Studio:

```bash
export JAVA_HOME=~/.local/opt/jdk17            # any JDK 17
export ANDROID_HOME=~/.local/opt/android-sdk   # contains cmdline-tools/latest
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" "build-tools;35.0.0" "platform-tools"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug testDebugUnitTest
```

## One-time phone setup

1. **Install the APK** (from your dev machine with a USB or wireless ADB connection):

   ```bash
   ./gradlew assembleRelease
   adb install app/build/outputs/apk/release/app-release.apk
   ```

2. **Allow battery optimization exemption**: Open the app. When it prompts, tap "Allow" on the
   "ignore battery optimization" dialog. This keeps audio playing overnight.

3. **Allow installing updates**: The first time an update is ready, the app asks you to grant
   "Install unknown apps" permission for YT Sleep. Open
   **Settings → Apps → Special access → Install unknown apps → YT Sleep → Allow**.
   This is a one-time step. After that, every update shows the system "Install" prompt
   automatically (you tap "Install" and it's done).

4. **Block Chrome, YouTube, and the Play Store** in AppBlock. You won't need them again.
   The app is self-contained and self-updating.

## Keystore (one time)

```bash
keytool -genkeypair -v \
  -keystore ~/keys/ytsleep-release.jks \
  -alias ytsleep \
  -keyalg RSA -keysize 4096 -validity 36500
```

Add to `~/.gradle/gradle.properties`:

```properties
YTSLEEP_STORE_FILE=/home/you/keys/ytsleep-release.jks
YTSLEEP_STORE_PASSWORD=yourpassword
YTSLEEP_KEY_ALIAS=ytsleep
YTSLEEP_KEY_PASSWORD=yourpassword
```

> ⚠️ **Back up the keystore** (e.g., encrypted copy in your password manager). If you lose it,
> you can't update the installed app — you'd have to uninstall it and lose your history.

## Publishing an update

```bash
./scripts/release.sh "One-line release note"
```

Releases go to `abdelwahabAmged/youtube-sleep` (override with `RELEASE_REPO=owner/repo`). The repo must be **public** so the app can fetch `update.json` without signing in.

On the phone, the next time you open the app with nothing playing, the system install prompt
appears by itself. One tap on **Install** updates the app.

## When YouTube breaks playback

1. Run `./gradlew dependencyUpdates` or check the NewPipeExtractor releases page.
2. Bump the extractor version in `gradle/libs.versions.toml`.
3. If InnerTube parsing broke, refresh the JSON fixtures in
   `app/src/test/resources/innertube/` and fix the parser.
4. Run `./scripts/release.sh "Fix YouTube playback"`. The phone gets it on its next launch.

## Running tests

```bash
./gradlew testDebugUnitTest          # JVM unit tests
./gradlew connectedDebugAndroidTest  # Instrumented tests (phone connected)
```
