# Contract: Screens and Navigation

All screens use the dark sleep theme (FR-014). There are no auto-advancing carousels.
Lists load their next page automatically while scrolling (FR-013). Every tappable item stays inside the app: there are no external
intents to a browser or to YouTube (FR-020).

```text
Start (opening screen, FR-015)
├── [Continue listening card]  ── 1 tap ──▶ resumes playback (Player sheet opens)
├── Recent (up to 20)          ── tap item ─▶ plays
├── My playlists               ── tap ─▶ Playlist ── tap item ─▶ plays queue from that item
├── [Update banner]            ── tap ─▶ installs update (only shown when an update is ready)
└── Bottom bar: Start · Feed · Search · Settings

Feed            tabs: Home | Subscriptions (FR-010) · list with automatic paging · tap ─▶ plays single item
Search          query box · results with automatic paging · tap ─▶ plays single item
Playlist        header (title, count) · items with automatic paging · "Play all" · tap item ─▶ queue from that item
Player (sheet)  thumbnail (static), title, channel, seek bar, elapsed/total,
                ⏮ −15s ⏯ +30s ⏭, repeat toggle (off/one/playlist), sleep timer button
Settings        Account (connect / reconnect / sign out / paste cookies), battery optimization,
                About (version name + code, "Check for updates")
Connect account full-screen in-app WebView sign-in (research R4); closes itself after success
```

## Tap budgets (SC-003)

| Goal | Taps from app launch |
|---|---|
| Resume last item | 1 (Continue listening) |
| Play a recent item | 1 |
| Play a playlist item | 2–3 (playlist → item, or playlist → Play all) |

## Empty, error and loading states

| State | Display |
|---|---|
| First launch, nothing played, not signed in | Start shows "Search" and "Connect your YouTube account" |
| Feed or Playlists when not signed in | Explanation plus a "Connect account" button. Search still works |
| Session expired | Same, with the text "Reconnect account" |
| Offline | "You're offline". Start still lists Recent from local history |
| Unplayable item | Short calm message. In a playlist queue, it moves to the next item automatically |
| Source changed | "YouTube changed something — an app update is needed" |
| Loading | A static dim placeholder (no shimmer animation) |
