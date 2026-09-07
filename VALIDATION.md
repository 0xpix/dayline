# Dayline v0.14.0.beta — Validation Report

This report records checks performed on the v0.14.0.beta source package. GitHub Actions remains the authoritative Android/Compose compile gate because this execution environment does not contain the Android SDK/Gradle toolchain used by the project.

## Release checks

- Static project validator: **PASS — 0 errors / 0 warnings**
- Kotlin sources discovered: **55**
- XML resources/manifests parsed: **28**
- GitHub Actions workflow YAML parse: **PASS**
- Beta/Play flavor and permission separation: **PASS**
- GitHub beta signing / explicit Build Tools `apksigner` contract: **PASS**
- Play signing/AAB workflow contract: **PASS**
- Proprietary Nothing `glyph-matrix-sdk-2.0.aar` absent from source tree: **PASS**
- GitHub beta workflow checks AAR absence before fetching the official SDK: **PASS**
- Play workflow does not fetch/package the Glyph Matrix SDK: **PASS**
- Main/Play manifest has no beta updater Internet/install permission and no Nothing Glyph permission: **PASS**
- Beta manifest contains Nothing Glyph permission, Glyph Toy service and AOD metadata: **PASS**
- Automatic Android app-data backup disabled: **PASS**
- Notification seconds-chronometer / `HH:mm:ss` regression scan: **PASS**
- Merge-marker / duplicate-import scan: **PASS**

## Glyph core validation

The Android-independent Glyph model and 13×13 pattern renderer compile with the local Kotlin compiler.

Runtime-independent tests passed for every defined Glyph signal:

- every matrix frame is exactly **13×13 / 169 cells**
- every brightness value is within **0..255**
- every state renders a non-empty pattern
- `LOOK_LEFT` has a lower horizontal light centroid than `CENTER`
- `LOOK_RIGHT` has a higher horizontal light centroid than `CENTER`
- `BLINK` uses fewer illuminated cells than open center eyes
- 32 eye/app-state patterns validated

Directional result: **5.0 < 6.0 < 7.0** for Left < Center < Right.

## v0.14 Glyph behavior

Dayline Glyph is designed to remain eye-dominant. Idle behavior supports Center, Curious, Sleepy and Happy base expressions, optional 4–9 second natural blinking and Rare / Normal / Frequent random glances.

Blink is explicitly animated **CENTER → BLINK → CENTER**. App-derived states are latched only when the state changes, displayed for the configured short duration, then the eye loop resumes. Focus can continue with focused/squint eyes and optional active brightness pulsing; Rest can fall back to Sleepy eyes or be disabled.

Brief signals include Next event, Reminder soon, Focus, Rest, Task done, Conflict, Free now, Day open, No plans, Event start/end, Moved, Sync OK/error, Missed and Go. A persistent priority/expiry queue lets app actions interrupt the AOD toy without permanently replacing the face.

Quiet hours, dim-at-night, reduce-motion and return-to-eyes settings are persisted in Dayline backups.

## Nothing Phone (4a) Pro integration

The beta flavor registers `DaylineGlyphToyService` as an AOD-capable Glyph Toy and targets the documented Phone (4a) Pro 13×13 matrix. The hardware bridge uses reflection so the open-source tree and Play flavor remain buildable without committing Nothing's proprietary AAR.

Nothing's documentation names `Glyph.DEVICE_25111p`; a public SDK issue reports that some current binaries do not expose that field. Dayline first attempts the field and falls back to the documented `25111p` registration target string.

The GitHub beta CI fetches `glyph-matrix-sdk-2.0.aar` directly from Nothing's official developer repository only after verifying the binary was not committed. The Play flavor intentionally excludes the SDK pending commercial licensing permission.

## Final Android compile gate

Before tagging the release, GitHub Actions must pass:

```text
:app:assembleBetaDebug
:app:assemblePlayDebug
```

For tag `v0.14.0.beta`, the tagged job additionally builds `:app:assembleBetaRelease`, verifies the APK with Android Build Tools 36.0.0 `apksigner`, generates the SHA-256 checksum and publishes the GitHub prerelease.

Do not treat v0.14.0.beta as Android-compile-confirmed until that workflow is green.
