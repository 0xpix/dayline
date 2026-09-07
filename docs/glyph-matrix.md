# Dayline Glyph Matrix integration

Dayline v0.14.6.beta provides an experimental **Nothing Phone (4a) Pro** Glyph Matrix experience. The device uses a **13×13** matrix and supports **AOD-only Glyph Toys**.

## Dayline behavior

Dayline is intentionally eyes-first. The live idle face keeps only the expressions that read cleanly on the 13×13 matrix: Center, Look left, Look right, Blink, Wink, Happy, Hearts, Squint and rare Sleepy.

Every non-center animation is isolated by Center on both sides. The runtime follows `CENTER → animation → CENTER → next animation`, including Blink. A dedicated ~700 ms Center recovery is enforced before any due blink or motion can start.

The Settings preview mirrors that same transition contract. It uses a single circular black preview surface and only draws illuminated matrix cells, so the old square-grid shape is no longer visible inside the circle.

Automatic calendar/app-state symbols do not interrupt the face. Focus Mode is the only automatic Dayline layout change.

## Focus timer layout

The old circular Focus progress ring has been removed.

When a Focus or Rest phase is active, Dayline uses a separate compact renderer:

- Rows near the top contain smaller expressive eyes.
- The same Center / left / right / blink / wink / happy / hearts / squint / sleepy animation state continues running.
- Look Left and Look Right shift the complete compact eye pair in the same direction as normal idle mode.
- Rows 5–7 are intentionally left empty as visual breathing room.
- Rows 8–12 are reserved for a fixed **`MM:SS` countdown**.
- A compact 3×5 numeric font uses all 13 columns exactly: two minute digits, a one-column colon and two second digits.
- Focus 25/5, 50/10 and custom cycles show the actual remaining time of the current phase.
- Rest uses the same clean countdown layout rather than a different symbol or progress ring.
- Paused sessions display the persisted paused remaining time.

Normal idle mode never uses the compact Focus eyes; its larger 5×5 rounded eyes remain unchanged.

Dayline keeps a simple 0–100% brightness control and maps it to the higher raw `IntArray` Matrix intensity range used by Nothing's official example project. Duplicate raw frames are suppressed to reduce visible flicker/twitching, so the countdown updates only when the visible second or eye expression changes.

## Activating the AOD toy

1. Install the GitHub **beta** APK.
2. In Dayline open **Settings → Glyph → Dayline Glyph**.
3. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
4. Tap **ACTIVATE IN NOTHING SETTINGS**.
5. In Nothing OS select **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Dayline Eyes**.

## Open-source / SDK boundary

Dayline's own Glyph patterns, settings and reflection bridge are part of the MIT-licensed source tree. Nothing's `glyph-matrix-sdk-2.0.aar` is **not** committed to or redistributed as a source artifact by this repository.

For GitHub beta CI, `.github/workflows/build-apk.yml` first validates that the AAR is absent and then downloads the official binary from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` before compiling the beta flavor. `app/build.gradle.kts` only attaches the AAR to `betaImplementation` when the file exists.

The Play flavor does not include the SDK in v0.14.6.beta. Nothing's Glyph SDK license restricts commercial use without written permission, so Play distribution should stay disabled for Glyph hardware until the appropriate permission/license is obtained.

## Release safety

Tagged beta builds are checked before publication. The signed APK's embedded `versionName` must match the Git tag, and its `versionCode` must match Dayline's beta version-code convention. This prevents a release tag from publishing an APK that Android considers the same or older than the intended beta.

## Compatibility note

Nothing's current documentation names the Phone (4a) Pro target `Glyph.DEVICE_25111p`. Dayline attempts the documented field first and falls back to the documented `25111p` target string at runtime for SDK builds that do not expose the constant.
