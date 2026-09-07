# Dayline Glyph Matrix integration

Dayline v0.15.2.beta provides an experimental **Nothing Phone (4a) Pro** Glyph Matrix experience. The device uses a **13×13** matrix and supports **AOD-only Glyph Toys**.

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

### v0.15 timer layout

The timing logic is unchanged; only readability is improved. A Focus time such as `14:54` is rendered as two large centered lines:

```text
14
54
```

- **Minutes** use two 5×5 digits in rows 1–5.
- **Seconds** use two 5×5 digits in rows 7–11.
- Each line is 11 pixels wide and centered with one empty column at each side.
- Leading zeros remain visible, so `05:07` displays `05` above `07`.
- No eyes, colon, progress ring or other pixels compete with the timer during the announcement.

## Reliability / freeze recovery

The v0.14.9 reliability layer remains unchanged in v0.15.2.beta.

Previously, a temporary SDK service disconnect could leave Dayline with `connected = false` while an old manager object was still present. New frames would then be queued, but the bridge would not initialize a fresh connection, so the hardware could remain frozen on the last visible frame indefinitely.

The bridge now:

- invalidates stale manager/callback state when the Nothing service disconnects;
- reconnects after disconnects, registration failures and frame-send failures;
- retains only the newest pending frame while reconnecting;
- ignores callbacks from older connection generations;
- does not tell the renderer that a queued/failed frame was successfully delivered.

The AOD service also protects its Handler render loop so an unexpected exception in one tick cannot terminate all later ticks. Stable frames are resent every **4 seconds** as a lightweight recovery heartbeat; normal changed animation/timer frames are still sent immediately.

## Glyph settings

The compact Glyph configuration introduced in v0.14.8 remains unchanged. The main Settings page contains one **Dayline Glyph** row. Opening it shows five groups:

- **Glyph** — enable state, hardware status and shortcut to Nothing Settings.
- **Look** — brightness, Blink, Expressions, Motion and Reduce motion.
- **Focus** — the 30-second checkpoint rule summarized as `Start · every 5 min · 1 min left`.
- **Night** — quiet hours, start/end time and optional dimming.
- **Test expressions** — Center, Left, Right, Blink, Happy, Wink, Hearts, Squint and Sleepy.

Dayline keeps a simple 0–100% brightness control and maps it to the higher raw `IntArray` Matrix intensity range used by Nothing's official example project. Duplicate raw frames are suppressed between recovery heartbeat frames to reduce visible flicker/twitching.

## Activating the AOD toy

1. Install the GitHub **beta** APK.
2. In Dayline open **Settings → Glyph → Dayline Glyph**.
3. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
4. Tap **OPEN NOTHING SETTINGS**.
5. In Nothing OS select **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Dayline Eyes**.

## Open-source / SDK boundary

Dayline's own Glyph patterns, settings and reflection bridge are part of the MIT-licensed source tree. Nothing's `glyph-matrix-sdk-2.0.aar` is **not** committed to or redistributed as a source artifact by this repository.

For GitHub beta CI, `.github/workflows/build-apk.yml` first validates that the AAR is absent and then downloads the official binary from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` before compiling the beta flavor. `app/build.gradle.kts` only attaches the AAR to `betaImplementation` when the file exists.

The Play flavor does not include the SDK in v0.15.2.beta. Nothing's Glyph SDK license restricts commercial use without written permission, so Play distribution should stay disabled for Glyph hardware until the appropriate permission/license is obtained.

## Release safety

Tagged beta builds are checked before publication. The signed APK's embedded `versionName` must match the Git tag, and its `versionCode` must match Dayline's beta version-code convention. This prevents a release tag from publishing an APK that Android considers the same or older than the intended beta.

## Compatibility note

Nothing's current documentation names the Phone (4a) Pro target `Glyph.DEVICE_25111p`. Dayline attempts the documented field first and falls back to the documented `25111p` target string at runtime for SDK builds that do not expose the constant.
