# Dayline Glyph Matrix integration

Dayline v0.15.4.beta provides an experimental **Nothing Phone (4a) Pro** Glyph Matrix experience. The device uses a **13×13** matrix and supports **AOD-only Glyph Toys**.

## Dayline behavior

Dayline is intentionally eyes-first. The live idle face keeps only the expressions that read cleanly on the 13×13 matrix: Center, Look left, Look right, Blink, Wink, Happy, Hearts, Squint and rare Sleepy.

Every non-center animation is isolated by Center on both sides. The runtime follows `CENTER → animation → CENTER → next animation`, including Blink. A dedicated ~700 ms Center recovery is enforced before any due blink or motion can start.

Automatic calendar/app-state symbols do not interrupt the face. Focus Mode is the only automatic Dayline timing behavior.

## Focus time announcements

The persistent Focus timer and old circular progress ring remain removed. Focus and Rest keep the **same normal large eyes** as idle mode for nearly the entire phase.

At selected checkpoints, Dayline temporarily replaces the eyes with a countdown:

- The current phase time is announced for **30 seconds**.
- The transition is always `eyes → CENTER → time → CENTER → eyes`.
- A phase always announces its **start time**.
- It then announces every **5-minute remaining checkpoint** below the start.
- It also announces **1:00 remaining**.
- The displayed time remains live during the 30-second window rather than freezing on the checkpoint value.
- Paused sessions keep their persisted remaining time frozen while an announcement is visible.

Examples:

- 25-minute Focus: `25:00`, `20:00`, `15:00`, `10:00`, `05:00`, `01:00`.
- 5-minute Rest: `05:00`, `01:00`.
- 50-minute Focus: `50:00`, `45:00`, `40:00`, `35:00`, `30:00`, `25:00`, `20:00`, `15:00`, `10:00`, `05:00`, `01:00`.
- Custom 12-minute Focus: `12:00`, `10:00`, `05:00`, `01:00`.

Outside those 30-second windows, Focus does not alter the face. The normal Center / left / right / blink / wink / happy / hearts / squint / sleepy animation state machine continues unchanged.

### v0.15.4 timer layout

The timing logic is unchanged. A Focus time such as `14:54` is still rendered as two centered lines:

```text
14
54
```

v0.15.4 reduces the digit width so the timer sits more comfortably inside the circular visible Matrix area:

- **Minutes** use two 4×5 digits in rows 1–5.
- **Seconds** use two 4×5 digits in rows 7–11.
- Each line is 9 pixels wide: `4 + 1 gap + 4`.
- Digits occupy x=2..10, leaving **two empty columns on both sides**.
- Leading zeros remain visible, so `05:07` displays `05` above `07`.
- No eyes, colon, progress ring or other pixels compete with the timer during the announcement.

## Reliability / freeze recovery

The conservative v0.15.3 transport remains unchanged in v0.15.4.beta.

Nothing's Matrix SDK owns a bound proxy service, so Dayline follows a conservative lifecycle:

- SDK callback state is serialized onto the **main looper** so binder callbacks cannot race the animation renderer.
- When `onServiceDisconnected` fires, Dayline keeps the existing manager/callback binding alive first and waits for the system proxy to reconnect naturally.
- If it remains disconnected for about **5 seconds**, Dayline performs a clean `unInit()` before initializing a fresh binding.
- Repeated recovery attempts back off up to **30 seconds** instead of reinitializing every 750 ms.
- Only the newest pending frame is retained while disconnected.
- Connection generations reject callbacks from a superseded binding.
- A real frame-send exception enters the same delayed recovery path instead of immediately starting a reconnect loop.
- Duplicate unchanged frames are suppressed inside `NothingGlyphBridge`, so the old service-level 4-second heartbeat request does **not** produce redundant `setMatrixFrame()` traffic on the hardware.

The AOD render loop is exception-protected so one unexpected render failure cannot permanently stop later animation ticks.

This design deliberately favors a stable long-lived SDK binding over frequent proactive reconnects.

## Glyph settings

The compact Glyph configuration introduced in v0.14.8 remains unchanged. The main Settings page contains one **Dayline Glyph** row. Opening it shows five groups:

- **Glyph** — enable state, hardware status and shortcut to Nothing Settings.
- **Look** — brightness, Blink, Expressions, Motion and Reduce motion.
- **Focus** — the 30-second checkpoint rule summarized as `Start · every 5 min · 1 min left`.
- **Night** — quiet hours, start/end time and optional dimming.
- **Test expressions** — Center, Left, Right, Blink, Happy, Wink, Hearts, Squint and Sleepy.

Dayline keeps a simple 0–100% brightness control and suppresses duplicate raw frames before they reach Nothing's SDK to reduce unnecessary Matrix traffic.

## Activating the AOD toy

1. Install the GitHub **beta** APK.
2. In Dayline open **Settings → Glyph → Dayline Glyph**.
3. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
4. Tap **OPEN NOTHING SETTINGS**.
5. In Nothing OS select **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Dayline Eyes**.

## Open-source / SDK boundary

Dayline's own Glyph patterns, settings and reflection bridge are part of the MIT-licensed source tree. Nothing's `glyph-matrix-sdk-2.0.aar` is **not** committed to or redistributed as a source artifact by this repository.

For GitHub beta CI, `.github/workflows/build-apk.yml` first validates that the AAR is absent and then downloads the official binary from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` before compiling the beta flavor. `app/build.gradle.kts` only attaches the AAR to `betaImplementation` when the file exists.

The Play flavor does not include the SDK in v0.15.4.beta. Nothing's Glyph SDK license restricts commercial use without written permission, so Play distribution should stay disabled for Glyph hardware until the appropriate permission/license is obtained.

## Release safety

Tagged beta builds are checked before publication. The signed APK's embedded `versionName` must match the Git tag, and its `versionCode` must match Dayline's beta version-code convention. This prevents a release tag from publishing an APK that Android considers the same or older than the intended beta.

## Compatibility note

Nothing's current documentation names the Phone (4a) Pro target `Glyph.DEVICE_25111p`. Dayline attempts the documented field first and falls back to the documented `25111p` target string at runtime for SDK builds that do not expose the constant.