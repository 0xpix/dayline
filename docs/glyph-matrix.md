# Dayline Glyph Matrix integration

Dayline v0.14.0.beta adds an experimental **Nothing Phone (4a) Pro** Glyph Matrix experience. The device uses a **13×13** matrix and supports **AOD-only Glyph Toys**.

## Dayline behavior

Dayline is intentionally eyes-first. The idle loop shows solid dot-matrix eyes, quick blinks and optional glances. Calendar/focus signals are short interruptions and then return to the face.

Core eye states: Center, Look left, Look right, Blink, Wink, Happy, Sleepy, Curious, Side-eye, Rolling, Squint and Hearts.

Core app states: Next event, Reminder soon, Focus, Rest, Task complete, Conflict, Free now / Day open, Event started/ended, Moved, Sync OK/error, Missed and Go.

The default profile uses Eyes + app states, rare glances, a 3-second state duration, a 5-second reminder signal, quiet hours 23:00–07:00, night dimming and automatic return to eyes.

## Activating the AOD toy

1. Install the GitHub **beta** APK.
2. In Dayline open **Settings → Glyph → Dayline Glyph**.
3. Choose **Eyes only** or **Eyes + states**.
4. Tap **ACTIVATE IN NOTHING SETTINGS**.
5. In Nothing OS select **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy → Dayline Eyes**.

The settings screen also contains an on-screen matrix preview and hardware test buttons. If the Nothing SDK/device is unavailable, the simulated preview still works.

## Open-source / SDK boundary

Dayline's own Glyph patterns, state mapping, settings and reflection bridge are part of the MIT-licensed source tree. Nothing's `glyph-matrix-sdk-2.0.aar` is **not** committed to or redistributed as a source artifact by this repository.

For GitHub beta CI, `.github/workflows/build-apk.yml` first validates that the AAR is absent and then downloads the official binary from `Nothing-Developer-Programme/GlyphMatrix-Developer-Kit` before compiling the beta flavor. `app/build.gradle.kts` only attaches the AAR to `betaImplementation` when the file exists.

The Play flavor does not include the SDK in v0.14.0.beta. Nothing's Glyph SDK license restricts commercial use without written permission, so Play distribution should stay disabled for Glyph hardware until the appropriate permission/license is obtained.

## Compatibility note

Nothing's current documentation names the Phone (4a) Pro target `Glyph.DEVICE_25111p`. A public SDK issue reports that some shipped SDK binaries do not yet expose that constant. Dayline therefore attempts the documented field first and falls back to the documented `25111p` target string at runtime.
