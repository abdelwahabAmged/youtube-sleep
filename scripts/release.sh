#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/release.sh "Release note"   (RELEASE_REPO defaults to abdelwahabAmged/youtube-sleep)
# Optional: VERSION_NAME=0.3.0 ./scripts/release.sh "Release note"

RELEASE_NOTE="${1:-}"
if [[ -z "$RELEASE_NOTE" ]]; then
  echo "Usage: ./scripts/release.sh \"Release note\"" >&2
  exit 1
fi

RELEASE_REPO="${RELEASE_REPO:-abdelwahabAmged/youtube-sleep}"

for tool in git gh jq sha256sum; do
  command -v "$tool" >/dev/null || { echo "ERROR: $tool is required" >&2; exit 1; }
done

# Check gh auth
if ! gh auth status &>/dev/null; then
  echo "ERROR: gh is not authenticated. Run: gh auth login" >&2
  exit 1
fi

# Check signing properties
GRADLE_PROPS="$HOME/.gradle/gradle.properties"
for key in YTSLEEP_STORE_FILE YTSLEEP_STORE_PASSWORD YTSLEEP_KEY_ALIAS YTSLEEP_KEY_PASSWORD; do
  if ! grep -q "^${key}=" "$GRADLE_PROPS" 2>/dev/null && [[ -z "${!key:-}" ]]; then
    echo "ERROR: Missing signing property: $key (set in $GRADLE_PROPS or env)" >&2
    exit 1
  fi
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

# Read current version from build.gradle.kts
CURRENT_VERSION_CODE=$(grep 'versionCode = ' app/build.gradle.kts | grep -oP '\d+')
CURRENT_VERSION_NAME=$(grep 'versionName = ' app/build.gradle.kts | grep -oP '"[0-9]+\.[0-9]+\.[0-9]+"' | tr -d '"')

NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

if [[ -n "${VERSION_NAME:-}" ]]; then
  NEW_VERSION_NAME="$VERSION_NAME"
else
  # Increment patch component
  IFS='.' read -r major minor patch <<< "$CURRENT_VERSION_NAME"
  NEW_VERSION_NAME="${major}.${minor}.$((patch + 1))"
fi

echo "Bumping: $CURRENT_VERSION_NAME ($CURRENT_VERSION_CODE) -> $NEW_VERSION_NAME ($NEW_VERSION_CODE)"

# Update build.gradle.kts
sed -i "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" app/build.gradle.kts
sed -i "s/versionName = \"$CURRENT_VERSION_NAME\"/versionName = \"$NEW_VERSION_NAME\"/" app/build.gradle.kts

# Build the release APK
./gradlew assembleRelease -PRELEASE_REPO="$RELEASE_REPO"

APK_SRC="app/build/outputs/apk/release/app-release.apk"
mkdir -p app/release
APK_DEST="app/release/youtube-sleep-${NEW_VERSION_NAME}.apk"
cp "$APK_SRC" "$APK_DEST"

# Compute SHA-256 and size
SHA256=$(sha256sum "$APK_DEST" | awk '{print $1}')
SIZE_BYTES=$(stat -c%s "$APK_DEST")
APK_URL="https://github.com/${RELEASE_REPO}/releases/download/v${NEW_VERSION_NAME}/youtube-sleep-${NEW_VERSION_NAME}.apk"
PUBLISHED_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

# Write update.json (jq escapes the note, so quotes or backslashes can't break the JSON)
jq -n \
  --argjson versionCode "$NEW_VERSION_CODE" \
  --arg versionName "$NEW_VERSION_NAME" \
  --arg apkUrl "$APK_URL" \
  --arg sha256 "$SHA256" \
  --argjson sizeBytes "$SIZE_BYTES" \
  --arg publishedAt "$PUBLISHED_AT" \
  --arg notes "$RELEASE_NOTE" \
  '{schema: 1, versionCode: $versionCode, versionName: $versionName, apkUrl: $apkUrl,
    sha256: $sha256, sizeBytes: $sizeBytes, minSdk: 29, publishedAt: $publishedAt, notes: $notes}' \
  > app/release/update.json

echo "update.json:"
cat app/release/update.json

# Record the version bump in git so the published code matches the release
git add app/build.gradle.kts
git commit -m "Release v${NEW_VERSION_NAME}" >/dev/null
git push

# Create GitHub release
gh release create "v${NEW_VERSION_NAME}" \
  "$APK_DEST" \
  app/release/update.json \
  --repo "$RELEASE_REPO" \
  --notes "$RELEASE_NOTE" \
  --latest

echo ""
echo "Released v${NEW_VERSION_NAME} (versionCode $NEW_VERSION_CODE)"
echo "The phone will download and prompt for install on next app launch."
