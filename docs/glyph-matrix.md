# Dayline Glyph Matrix integration

Dayline v0.14.3.beta provides an experimental **Nothing Phone (4a) Pro** Glyph Matrix experience. The device uses a **13×13** matrix and supports **AOD-only Glyph Toys**.

## Dayline behavior

Dayline is intentionally eyes-first. The Glyph stays on the face instead of being replaced by calendar-state icons.

Live eye states: **Center, Look left, Look right, Blink, Wink, Happy, Hearts, Squint and Sleepy**.

Curious, Playful, Surprised, Side-eye, Excited and Rolling were removed from the live loop because they did not read cleanly on the 13×13 Phone (4a) Pro matrix.

Look left/right always return through **Center** before another expression begins. This prevents a side glance from jumping directly into Happy, Wink or Blink.

Blinking is frequent, Happy appears often, and Sleepy is deliberately rare.

## Focus Mode

Focus Mode is the only automatic Dayline overlay.

The eyes continue animating while a circular pixel path around the face shows progress for:

- 25 / 5
- 50 / 10
- custom focus/rest cycles

The full perimeter remains faintly visible, then completed pixels brighten one by one. Focus advances clockwise and break advances in reverse.

## Brightness

Dayline exposes a simple 0–100% brightness control in Settings.

The Glyph Toy uses the raw `IntArray` Matrix API. Nothing's official Glyph Matrix example project demonstrates raw values up to 2046, so Dayline maps its 0–100% UI scale to the higher raw Matrix intensity range internally instead of restricting raw frames to the 0–255 object-builder brightness range.

Quiet-hours dimming is applied after that mapping.

## Activating the AOD toy

1. Install the GitHub **beta** APK.
2. In Dayline open **Settings → Glyph → Dayline Glyph**.
3. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
4. Tap **ACTIVATE IN NOTHING SETTINGS**.
5. In Nothing OS select **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Dayline Eyes**.

The settings screen contains an on-screen matrix preview plus compact test controls for the retained expressions. If the Nothing SDK/device is unavailable, the simulated preview still works.

## Open-source / SDK boundary

Dayline's own Glyph patterns, focus mapping, settings and reflection bridge are part of the MIT-licensed source tree. Nothing's `glyph-matrix-sdk-2.0.aar` is **not** committed to or redistributed as a source artifact by this repository.

For GitHub beta CI, `.github/workflows/build-apk.yml` first validates that the AAR is absent and then downloads the official binary from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` before compiling the beta flavor. `app/build.gradle.kts` only attaches the AAR to `betaImplementation` when the file exists.

The Play flavor does not include the SDK in v0.14.3.beta. Nothing's Glyph SDK license restricts commercial use without written permission, so Play distribution should stay disabled for Glyph hardware until the appropriate permission/license is obtained.

## Compatibility note

Nothing's current documentation names the Phone (4a) Pro target `Glyph.DEVICE_25111p`. Some SDK builds have shipped without that field. Dayline therefore attempts the documented field first and falls back to the documented `25111p` target string at runtime.
