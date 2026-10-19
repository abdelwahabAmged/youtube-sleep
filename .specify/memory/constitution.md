<!--
Sync Impact Report
==================
Version change: 1.0.0 → 1.1.0
Amendment (1.1.0): PWA no longer mandated; install technology left to the plan. Added an
  easy-update requirement (Principle III + Platform & Technical Constraints "Updates").
Initial adoption (1.0.0): all template placeholders replaced
  - [PRINCIPLE_1_NAME] → I. Audio-Only Playback
  - [PRINCIPLE_2_NAME] → II. No Shorts, No Ads
  - [PRINCIPLE_3_NAME] → III. Background & Screen-Off Playback (Mobile-First)
  - [PRINCIPLE_4_NAME] → IV. Zero-Cost by Default
  - [PRINCIPLE_5_NAME] → V. Personal Scope & Simplicity
Added principles:
  - VI. Sleep-First Experience
Added sections:
  - Platform & Technical Constraints (was [SECTION_2_NAME])
  - Development Workflow & Quality Gates (was [SECTION_3_NAME])
Removed sections: none
Templates requiring updates: none modified by this command (templates read the
  constitution at runtime).
Follow-up TODOs: none
-->

# YouTube Sleep Constitution

## Core Principles

### I. Audio-Only Playback

The app exists to let the user *listen* to YouTube content, not watch it.

- The UI MUST NOT render video frames at any point during playback.
- Visual elements MUST be limited to what is needed to find and control audio: titles,
  channel names, optional small static thumbnails in lists, and playback controls.
- Any approach that requires a visible video element to keep audio playing MUST hide it
  fully (zero visible area) or be rejected in favor of an audio-stream approach.

**Rationale**: Visual stimulation is what keeps the user awake; removing video removes the
reason to keep looking at the screen.

### II. No Shorts, No Ads

- YouTube Shorts MUST NOT appear anywhere in the app: not in search results, feeds,
  recommendations, channel listings, or playback queues. Content MUST be filtered by
  reliable signals (e.g. `/shorts/` URLs, Shorts shelves, duration thresholds) and the
  filter MUST be covered by tests.
- Advertisements (pre-roll, mid-roll, banners, sponsored cards) MUST NOT be played or shown.
- The app MUST NOT add any ads, promotions, or engagement prompts of its own.

**Rationale**: Shorts and ads are the two main sources of compulsive scrolling and
interruptions that defeat the goal of falling asleep.

### III. Background & Screen-Off Playback (Mobile-First)

- The primary target is a mobile phone; every feature MUST be designed and verified on a
  phone-sized viewport first. Desktop support is optional.
- The app MUST be installable on the phone as an app (home-screen icon, opens without
  browser chrome) through a simple, free mechanism. The specific technology (PWA, native
  wrapper, sideloaded APK, etc.) is a plan-level decision, not mandated here.
- New versions MUST reach the phone easily: ideally automatically on next launch, or at most
  via a single simple user action, with no manual rebuild/reinstall steps on the phone.
- Audio MUST continue when the screen is locked/off and when the app is in the background.
  This is the core acceptance criterion; a release that breaks it MUST NOT ship.
- Playback MUST expose lock-screen / notification controls (e.g. Media Session API) for
  play, pause, and seek where the platform allows.
- Known platform limitations (e.g. iOS PWA background-audio restrictions) MUST be
  documented in the relevant spec/plan along with the chosen workaround.

**Rationale**: The user listens in bed with the screen off; if audio stops when the screen
locks, the app has failed its only job.

### IV. Zero-Cost by Default

- Every dependency, API, hosting platform, and service MUST be free for the expected
  personal usage volume (free tiers, open-source, self-hosting on owned hardware).
- Any component that could incur cost (paid API quota, paid hosting, app-store fees) MUST be
  explicitly justified in the plan and approved by the user before adoption.
- Designs SHOULD prefer client-side or static-hosting solutions over always-on servers.

**Rationale**: This is a personal project with no budget; recurring costs are not acceptable.

### V. Personal Scope & Simplicity

- The app serves a single user. Multi-user features, accounts systems, analytics,
  monetization, and public-launch concerns are out of scope unless the constitution is
  amended.
- Prefer the simplest solution that satisfies Principles I–IV and VI (YAGNI). New
  frameworks, services, or layers MUST be justified by a concrete need.
- Content obtained from YouTube MUST be used only for personal playback and MUST NOT be
  redistributed or publicly hosted.

**Rationale**: Keeping scope personal keeps the project small, free, and maintainable by one
person.

### VI. Sleep-First Experience

- The UI MUST default to a dark, low-brightness theme with no bright flashes or animations.
- The app MUST NOT include engagement-driving features (infinite feeds, autoplay of
  unrelated recommendations, notifications nudging the user to open the app).
- Interactions needed to start listening MUST be minimal so the user can put the phone down
  quickly.

**Rationale**: Every design decision is judged by whether it helps the user stop looking at
the phone and fall asleep.

## Platform & Technical Constraints

- **Target**: The user's phone, used as an installed app. The delivery technology is not
  fixed; the plan MUST pick the simplest free option that satisfies Principle III,
  including easy delivery of new features. Must work on the user's actual device/OS,
  which MUST be stated in the first feature spec.
- **Updates**: Shipping a new feature MUST NOT require more than publishing it from the dev
  machine plus at most one simple action on the phone. The update path MUST be described
  in the plan.
- **Hosting**: Free static hosting or free-tier platforms only (see Principle IV).
- **Content source**: YouTube content accessed in a way that yields audio without video,
  Shorts, or ads (see Principles I–II). The chosen method and its fragility/maintenance risk
  MUST be documented in the plan.
- **Privacy**: No third-party tracking or analytics. Any user data (history, favorites,
  settings) SHOULD be stored locally on the device.

## Development Workflow & Quality Gates

- Work follows the Spec Kit flow: specify → (clarify) → plan → tasks → implement.
- Every plan MUST include a Constitution Check confirming compliance with Principles I–VI;
  violations MUST be listed with justification or the design changed.
- Before a feature is considered done it MUST be verified on the phone for:
  1. no video displayed,
  2. no Shorts or ads present,
  3. audio continues with the screen locked for at least 10 minutes,
  4. total running cost remains $0.
- Automated tests SHOULD cover content filtering (Shorts/ads) and playback state logic;
  manual on-device checks cover background playback.

## Governance

- This constitution supersedes other practices and preferences for this project.
- Amendments are made by the project owner via `/speckit-constitution`, recorded with an
  updated version and amendment date.
- Versioning follows semantic versioning:
  - MAJOR: removing or redefining a principle in a backward-incompatible way.
  - MINOR: adding a principle/section or materially expanding guidance.
  - PATCH: clarifications and wording fixes.
- Every spec, plan, and task list MUST be checked against this constitution; unresolved
  conflicts block implementation until the design or the constitution is changed.

**Version**: 1.1.0 | **Ratified**: 2026-10-19 | **Last Amended**: 2026-10-19
