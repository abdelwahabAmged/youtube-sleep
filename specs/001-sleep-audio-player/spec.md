# Feature Specification: Sleep Audio Player for YouTube

**Feature Branch**: `001-sleep-audio-player`

**Created**: 2026-10-19

**Status**: Draft

**Input**: User description: "Develop Youtube for sleep is an app should help me have a better sleep by allowing me to listen to youtube audios only with no videos and with no shorts and no adds, the app users will be only me, i will mainly use it on my android phone. I should be able to scrolle on my youtube feed and search and open my playlist with all the thumbnials of youtube exist but when play the video should no video show up just the audio and no add or shorts show and should b able play the video on the background and close the mobile screen. also i should be able to get the updates and featuer I make easy on my phone when i develop a new thing. and should not be tight to chrome or anything if just to install it once or something okay but not to tight to other apps becaus i use appblock on my phone and i block alot of apps like chrome and youtube etc... so just little chance to be able to use other apps for our app and then I block it"

## Clarifications

### Session 2026-10-19

- Q: Which YouTube feed should the Feed screen show? → A: Both, as two separate tabs: Home (personalized recommendations) and Subscriptions (latest uploads from followed channels).
- Q: What should happen when a single video or a whole playlist finishes playing? → A: Stop by default, with a repeat toggle (off / repeat one / repeat playlist).
- Q: Should you be able to download audio to the phone so it plays without internet? → A: No; streaming only for this feature. Offline downloads may be a later, separate feature.
- Q: Which videos should count as Shorts and be hidden? → A: Items YouTube marks as Shorts, plus any video 3 minutes (180 seconds) or shorter, everywhere including the user's own playlists.
- Q: What should the app show first when you open it? → A: A start screen with a "Continue listening" card, recently played items, and the user's playlists.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Listen to a video as audio with the screen off (Priority: P1)

In bed, the user opens the app, finds a video (by searching), taps it, and hears only its audio. No video picture is ever shown and no ad plays. The user locks the phone and puts it down; the audio keeps playing until the video ends or the user stops it from the lock screen.

**Why this priority**: This is the reason the app exists. Search plus audio-only, ad-free, screen-off playback is already a usable product on its own.

**Independent Test**: On the phone, search for a known long video (e.g. a 1-hour rain sound), start it, lock the screen, and confirm audio continues for at least 10 minutes with no video frame shown and no ad heard at any point.

**Acceptance Scenarios**:

1. **Given** the app is open, **When** the user searches for a term and taps a result, **Then** audio starts playing and the screen shows only the title, channel, thumbnail and playback controls, never moving video.
2. **Given** audio is playing, **When** the user locks the phone or switches to another app, **Then** audio continues without interruption.
3. **Given** audio is playing with the screen locked, **When** the user uses the lock-screen / notification controls, **Then** they can pause, resume, and skip forward/back within the track.
4. **Given** the video normally carries ads on YouTube, **When** the user plays it in the app, **Then** no ad audio or ad visuals are presented before, during, or after the content.
5. **Given** the user searches for any term, **When** results are displayed, **Then** no Shorts appear in the results.

---

### User Story 2 - Browse my feed and my playlists (Priority: P2)

The user connects their own YouTube account once. After that they can browse their YouTube feed (a Home tab with personalized recommendations and a Subscriptions tab with the latest uploads from followed channels) and open their own playlists (including saved/"Watch later" style lists where available), seeing each item with its usual thumbnail, title, channel and duration, and play any item as audio. Playing a playlist continues to the next item in that playlist automatically.

**Why this priority**: The user listens to content they have already curated or that YouTube recommends to them; without this they must search every time. It depends on the playback from Story 1.

**Independent Test**: Sign in once, open the feed and confirm it shows the user's personalized items with thumbnails and no Shorts; open a playlist, play it, lock the screen and confirm it moves to the next item on its own.

**Acceptance Scenarios**:

1. **Given** the user has connected their account, **When** they open the Feed screen, **Then** they see two tabs, Home (personalized recommendations) and Subscriptions (latest uploads from followed channels, newest first), each with thumbnails and with no Shorts and no sponsored/ad items.
2. **Given** the feed is shown, **When** the user scrolls near the end of the loaded items, **Then** more items load automatically; if loading fails, a "Try again" action is shown.
3. **Given** the user has connected their account, **When** they open the Playlists screen, **Then** they see all their playlists with cover thumbnails and can open any one to see its items.
4. **Given** a playlist is playing with the screen off, **When** the current item ends, **Then** the next non-Short item in the playlist starts automatically; when the last item ends, playback stops unless repeat is on (no unrelated autoplay).
6. **Given** repeat is set to "repeat one" or "repeat playlist", **When** the current item or the last playlist item ends, **Then** the same item or the playlist restarts from the beginning, continuing until the user stops it or the sleep timer ends.
5. **Given** the user has not connected their account, **When** they open Feed or Playlists, **Then** the app explains that sign-in is needed and offers to start it, while Search still works.

---

### User Story 3 - Get new versions on the phone easily (Priority: P2)

When the user (who is also the developer) finishes a new feature, they publish it from their development machine. The phone picks up the new version automatically the next time the app is opened, or after at most one simple tap, without reinstalling, without using a computer cable, and without needing any blocked app such as a browser or the YouTube app.

**Why this priority**: The user will keep developing the app; if updating is painful they will stop improving it. It is independent of the listening features and can be verified with a trivial change.

**Independent Test**: Install the app once. Publish a visible change (e.g. a version number on the settings screen). Open the app on the phone and confirm the new version is active after at most one tap, with the browser and YouTube apps blocked by the user's app blocker.

**Acceptance Scenarios**:

1. **Given** a newer version has been published, **When** the user opens the app, **Then** the update has already been downloaded and the system install prompt appears by itself (only while nothing is playing). The user's only action is one tap on "Install".
4. **Given** the install prompt appeared, **When** the user dismisses it or audio is playing, **Then** the app keeps working on the current version and shows a calm "Update ready" banner; tapping it brings back the install prompt.
2. **Given** an update is applied, **When** the app restarts, **Then** the user's sign-in, settings and history are preserved.
3. **Given** the phone is offline when the app opens, **When** an update check cannot run, **Then** the app still opens the current version normally.

---

### User Story 4 - Works while other apps are blocked (Priority: P1)

The user blocks apps such as the browser, the YouTube app and others with an app blocker. The app must run as its own standalone app on the phone so that everything (browsing, signing in, playing, updating) works with those apps blocked. Using another app is acceptable only for a one-time setup step (e.g. first install), after which that app can be blocked permanently.

**Why this priority**: If the app relies on a blocked app, the user cannot use it at all, or must unblock the very apps they are trying to avoid. This is a hard constraint on every other story.

**Independent Test**: After first install, block the browser(s), the YouTube app and the app store in the app blocker, then perform Stories 1–3 end to end and confirm none of them requires opening a blocked app.

**Acceptance Scenarios**:

1. **Given** the app is installed and the browser and YouTube apps are blocked, **When** the user launches the app from its home-screen icon, **Then** it opens as its own app (own icon, own entry in the app switcher, no browser address bar) and works fully.
2. **Given** blocked apps, **When** the user signs in to their YouTube account, **Then** sign-in completes inside the app without switching to a blocked app.
3. **Given** blocked apps, **When** the user taps any link or item in the app, **Then** nothing opens in the browser or YouTube app.

---

### User Story 5 - Sleep timer (Priority: P3)

The user can set a timer (e.g. 15, 30, 45, 60, 90 minutes, or "end of current item") after which audio gently fades out and stops.

**Why this priority**: Helps sleep and saves battery, but the app is useful without it.

**Independent Test**: Set a 15-minute timer, lock the screen, and confirm audio fades and stops at about 15 minutes.

**Acceptance Scenarios**:

1. **Given** audio is playing, **When** the user sets a 30-minute timer and locks the phone, **Then** audio fades out over the last few seconds and stops at 30 minutes (±1 minute).
2. **Given** a timer is running, **When** the user cancels it, **Then** playback continues normally.

---

### Edge Cases

- A video cannot be played as audio (removed, private, region-locked, age-restricted, live stream): the app shows a short, calm message and, inside a playlist, skips to the next item.
- A result or playlist item is a Short (marked as a Short by YouTube, or 3 minutes or shorter): it is hidden everywhere, including inside the user's own playlists.
- A video with an unknown duration (e.g. a live stream or a premiere) is not treated as a Short by the duration rule; it is shown only if it is otherwise playable.
- Network drops while the screen is off: playback continues from what is already loaded and resumes when the connection returns; if it cannot, it stops quietly rather than looping errors.
- The account sign-in expires: Feed and Playlists show a prompt to reconnect; Search and playback keep working.
- YouTube changes how it delivers content and playback breaks: the app shows a clear error rather than silently failing, so the user knows to ship a fix (via Story 3).
- Another app takes audio focus (incoming call, alarm): playback pauses and the user can resume it from the lock screen afterwards.
- Very long videos (8–10 hours of sleep sounds): playback works for the full length with the screen off.
- The app is closed by the phone's battery optimizer while playing: this must not happen during active playback.
- First launch with no history and no sign-in: the start screen shows no "Continue listening" card, offers Search, and suggests connecting the account to see playlists.

## Requirements *(mandatory)*

### Functional Requirements

**Playback**

- **FR-001**: The app MUST play YouTube content as audio only; no video frames may be shown at any time.
- **FR-002**: The app MUST NOT play or display advertisements of any kind (pre-roll, mid-roll, banners, sponsored items).
- **FR-003**: Audio MUST keep playing when the screen is locked/off and when the app is in the background, for the full length of the item (including items of 10+ hours).
- **FR-004**: The app MUST provide playback controls on the lock screen and in the notification area: play, pause, seek back/forward, and next/previous when in a playlist.
- **FR-005**: The in-app player MUST show title, channel, thumbnail, elapsed/total time and a seek bar.
- **FR-006**: When playing a playlist, the app MUST advance to the next playable, non-Short item automatically and MUST stop after the last item by default (no autoplay of unrelated content).
- **FR-006a**: The player MUST offer a repeat toggle with three modes: off (default), repeat one (loop the current item), and repeat playlist (restart the playlist after its last item). The chosen mode MUST be remembered between sessions.
- **FR-007**: The app MUST pause on interruptions (calls, alarms) and allow resuming afterwards.

**Discovery**

- **FR-008**: Users MUST be able to search YouTube and see results with thumbnail, title, channel and duration.
- **FR-009**: Users MUST be able to connect their own YouTube/Google account from inside the app, once, and stay signed in across restarts and updates.
- **FR-010**: Signed-in users MUST be able to view their feed as two separate tabs, both with thumbnails: Home (personalized YouTube recommendations) and Subscriptions (latest uploads from followed channels, newest first).
- **FR-011**: Signed-in users MUST be able to view all their playlists and the items within each, with thumbnails.
- **FR-012**: Shorts MUST be excluded from search results, both feed tabs, playlists (including the user's own) and any queue. An item counts as a Short if YouTube marks it as a Short OR its duration is 180 seconds or less.
- **FR-013**: Lists (feed, search, playlists) MUST load in pages; the next page loads automatically as the user scrolls near the end (infinite scroll, owner's choice 2026-10-19). A page that fails to load shows the error with a "Try again" action instead of retrying in a loop.
- **FR-013a**: Signed-in users MUST be able to save any listed video to Watch later or to one of their own playlists from the video's row, in at most 2 taps; the change is made on their YouTube account. "Liked videos" is not offered as a target.

**Sleep experience**

- **FR-014**: The app MUST use a dark, low-brightness interface with no bright flashes or attention-grabbing animations.
- **FR-015**: The app MUST open on a start screen showing a "Continue listening" card (the last played item at its saved position), recently played items, and the user's playlists. Starting playback from this screen MUST take no more than 1 tap for "Continue listening" and no more than 3 taps for any playlist item or recently played item. Feed and Search are reachable from this screen but are never the opening screen.
- **FR-016**: The app MUST offer a sleep timer with preset durations and an "end of current item" option, fading audio out before stopping.
- **FR-017**: The app MUST NOT send notifications other than the playback controls.

**Installation, independence and updates**

- **FR-018**: The app MUST be installed on the user's Android phone as a standalone app with its own home-screen icon and app-switcher entry.
- **FR-019**: After first installation, every feature (sign-in, browsing, playback, updates) MUST work with the phone's browsers, the YouTube app and the app store blocked by an app blocker. Using another app is allowed only during one-time initial setup.
- **FR-020**: No link or action inside the app may hand off to an external app.
- **FR-021**: New versions published by the developer MUST reach the phone with exactly one user action per update. The app downloads the update in the background and, on the next launch while nothing is playing, opens the system install prompt automatically; the only action is confirming "Install". No reinstall, cable, or computer is needed on the phone side. Granting the app permission to install updates is a one-time step done during initial setup.
- **FR-022**: Updates MUST preserve sign-in, settings, and history.
- **FR-023**: The app MUST display its current version number in a settings/about screen.

**Data and cost**

- **FR-024**: The app MUST remember recently played items and the playback position of each, so the user can resume where they left off.
- **FR-025**: User data (account connection, history, settings) MUST stay on the phone or in the user's own account; no third-party tracking or analytics.
- **FR-026**: Running, hosting, and updating the app MUST cost nothing for a single user's usage.

### Key Entities

- **Media item**: A single YouTube video playable as audio; title, channel, thumbnail, duration, whether YouTube marks it as a Short, playability status. It is hidden as a Short if marked so or if its duration is 180 seconds or less.
- **Playlist**: A user-owned ordered list of media items; name, cover thumbnail, item count.
- **Feed page**: A finite batch of media items for the signed-in user, from either the Home or the Subscriptions feed.
- **Playback session**: The current item, queue (if from a playlist), position, play/pause state, repeat mode (off / one / playlist), and active sleep timer.
- **Listening history entry**: A previously played media item with last position and last-played time.
- **Account connection**: The user's link to their YouTube account, kept on the device.
- **App version**: The installed version identifier and whether a newer one is available.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the screen locked, audio plays continuously for at least 60 minutes (and for the full length of a 10-hour item) without stopping, in 10 out of 10 test nights.
- **SC-002**: Across 50 played items and 20 browsed pages of feed/search/playlists, zero video frames, zero ads, and zero Shorts are seen or heard.
- **SC-003**: From tapping the app icon, the user can resume the last item in 1 tap, and start any playlist item or recent item in under 10 seconds and no more than 3 taps.
- **SC-004**: A change published by the developer is running on the phone within one app launch and exactly 1 tap (the "Install" confirmation), with no reinstall.
- **SC-005**: With browsers, the YouTube app and the app store blocked, 100% of the app's features remain usable.
- **SC-006**: Monthly cost of running and updating the app is $0.
- **SC-007**: The user reports, after two weeks of use, that they no longer need to unblock YouTube or a browser to listen before sleep.

## Assumptions

- **Device**: The user's phone runs **Android 11** (confirmed). The app supports Android 10 and later. On Android 11, Android always asks the user to confirm each self-update, which is why the update flow is built around that single confirmation (FR-021). iOS and desktop are out of scope for this feature.
- **Single user**: Only the owner uses the app; no multi-user accounts, sharing, or public distribution.
- **Account**: The user is willing to sign in to their own YouTube/Google account once inside the app to access the personalized feed and playlists. "My playlists" includes the user's created and saved playlists, plus "Liked videos" / "Watch later" if YouTube makes them available.
- **Feed**: "My YouTube feed" means both the Home (personalized recommendations) and Subscriptions feeds, shown as separate tabs. Comments, likes, and uploads are out of scope for this feature.
- **One-time setup**: Using another app (e.g. a browser or file manager) once for the initial install is acceptable; after that it can be blocked. Initial setup also includes allowing the app to install updates ("Install unknown apps" in system settings) and exempting it from battery optimization.
- **Phone settings**: The user can exempt the app from battery optimization once, if the phone requires it for uninterrupted background audio.
- **Fragility**: Access to YouTube content may break when YouTube changes its systems; the easy update path (Story 3) is the mitigation, and occasional fixes are expected.
- **Usage**: Content is used only for personal listening and is never redistributed.
- **Network**: Playback streams over an internet connection. Downloading audio for offline listening is out of scope for this feature (a possible later feature).
