# Dayline Glyph Matrix integration

Dayline v0.14.3.beta adds an experimental **Nothing Phone (4a) Pro** Glyph Matrix experience. The device uses a **13×13** matrix and supports **AOD-only Glyph Toys**.

## Dayline behavior

Dayline is intentionally eyes-first. The live face keeps only the expressions that read cleanly on the 13×13 matrix: Center, Look left, Look right, Blink, Wink, Happy, Hearts, Squint and rare Sleepy.

Every non-center animation is isolated by a Center frame on both sides. The runtime therefore follows `CENTER → animation → CENTER → next animation`, including Blink, so expressions never visually blend together.

Automatic calendar/app-state symbols no longer interrupt the face. Focus Mode is the only automatic Dayline overlay: a circular pixel progress path stays around the eyes while the eyes continue animating. The path fills clockwise during focus and in reverse during breaks for 25/5, 50/10 and custom cycles.

Dayline keeps a simple 0–100% brightness control and maps it to the higher raw `IntArray` Matrix intensity range used by Nothing's official example project. Duplicate raw frames are suppressed to reduce visible flicker/twitching.

## Activating the AOD toy

1. Install the GitHub **beta** APK.
2. In Dayline open **Settings → Glyph → Dayline Glyph**.
3. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
4. Tap **ACTIVATE IN NOTHING SETTINGS**.
5. In Nothing OS select **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Dayline Eyes**.

The settings screen also contains an on-screen matrix preview and hardware test buttons. If the Nothing SDK/device is unavailable, the simulated preview still works.

## Open-source / SDK boundary

Dayline's own Glyph patterns, state mapping, settings and reflection bridge are part of the MIT-licensed source tree. Nothing's `glyph-matrix-sdk-2.0.aar` is **not** committed to or redistributed as a source artifact by this repository.

For GitHub beta CI, `.github/workflows/build-apk.yml` first validates that the AAR is absent and then downloads the official binary from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` before compiling the beta flavor. `app/build.gradle.kts` only attaches the AAR to `betaImplementation` when the file exists.

The Play flavor does not include the SDK in v0.14.3.beta. Nothing's Glyph SDK license restricts commercial use without written permission, so Play distribution should stay disabled for Glyph hardware until the appropriate permission/license is obtained.

## Compatibility note

Nothing's current documentation names the Phone (4a) Pro target `Glyph.DEVICE_25111p`. Dayline attempts the documented field first and falls back to the documented `25111p` target string at runtime for SDK builds that do not expose the constant.
