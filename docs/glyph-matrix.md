# Dayline Glyph Matrix integration

Dayline v0.14.7.beta provides an experimental **Nothing Phone (4a) Pro** Glyph Matrix experience. The device uses a **13×13** matrix and supports **AOD-only Glyph Toys**.

## Dayline behavior

Dayline is intentionally eyes-first. The live idle face keeps only the expressions that read cleanly on the 13×13 matrix: Center, Look left, Look right, Blink, Wink, Happy, Hearts, Squint and rare Sleepy.

Every non-center animation is isolated by Center on both sides. The runtime follows `CENTER → animation → CENTER → next animation`, including Blink. A dedicated ~700 ms Center recovery is enforced before any due blink or motion can start.

The Settings preview mirrors that same transition contract. It uses a single circular black preview surface and only draws illuminated matrix cells, so the old square-grid shape is no longer visible inside the circle.

Automatic calendar/app-state symbols do not interrupt the face. Focus Mode is the only automatic Dayline timing behavior.

## Focus time announcements

The persistent Focus timer and the old circular progress ring are both removed. Focus and Rest now keep the **same normal large eyes** as idle mode for nearly the entire phase.

At selected checkpoints, Dayline temporarily replaces the eyes with a centred `MM:SS` countdown:

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

Outside those 30-second windows, Focus does not alter the face at all. The normal Center / left / right / blink / wink / happy / hearts / squint / sleepy animation state machine continues unchanged.

The announcement timer uses a compact 3×5 numeric font that fits `MM:SS` across all 13 columns and is vertically centred in the Matrix because no eye pixels are drawn at the same time.

Dayline keeps a simple 0–100% brightness control and maps it to the higher raw `IntArray` Matrix intensity range used by Nothing's official example project. Duplicate raw frames are suppressed to reduce visible flicker/twitching.

## Activating the AOD toy

1. Install the GitHub **beta** APK.
2. In Dayline open **Settings → Glyph → Dayline Glyph**.
3. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
4. Tap **ACTIVATE IN NOTHING SETTINGS**.
5. In Nothing OS select **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Dayline Eyes**.

## Open-source / SDK boundary

Dayline's own Glyph patterns, settings and reflection bridge are part of the MIT-licensed source tree. Nothing's `glyph-matrix-sdk-2.0.aar` is **not** committed to or redistributed as a source artifact by this repository.

For GitHub beta CI, `.github/workflows/build-apk.yml` first validates that the AAR is absent and then downloads the official binary from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` before compiling the beta flavor. `app/build.gradle.kts` only attaches the AAR to `betaImplementation` when the file exists.

The Play flavor does not include the SDK in v0.14.7.beta. Nothing's Glyph SDK license restricts commercial use without written permission, so Play distribution should stay disabled for Glyph hardware until the appropriate permission/license is obtained.

## Release safety

Tagged beta builds are checked before publication. The signed APK's embedded `versionName` must match the Git tag, and its `versionCode` must match Dayline's beta version-code convention. This prevents a release tag from publishing an APK that Android considers the same or older than the intended beta.

## Compatibility note

Nothing's current documentation names the Phone (4a) Pro target `Glyph.DEVICE_25111p`. Dayline attempts the documented field first and falls back to the documented `25111p` target string at runtime for SDK builds that do not expose the constant.
