# Dayline v0.14.5.beta — Validation Report

This report tracks the current v0.14.5.beta source state. GitHub Actions remains the authoritative Android/Compose compile gate.

## Release checks

- Static project validator: **PASS**
- Beta/Play flavor and permission separation: **PASS**
- GitHub beta signing / explicit Build Tools `apksigner` contract: **PASS**
- Tagged beta workflow now verifies the signed APK's embedded `versionName` and `versionCode` against the Git tag before publication.
- GitHub beta updater now requires a release to be newer by both semantic version and Android `versionCode`.
- Proprietary Nothing `glyph-matrix-sdk-2.0.aar` absent from source tree: **PASS**
- GitHub beta workflow checks AAR absence before fetching the official SDK: **PASS**
- Play workflow does not fetch/package the Glyph Matrix SDK: **PASS**
- Main/Play manifest has no beta updater Internet/install permission and no Nothing Glyph permission: **PASS**
- Beta manifest contains Nothing Glyph permission, Glyph Toy service and AOD metadata: **PASS**
- Automatic Android app-data backup disabled: **PASS**
- Notification seconds-chronometer / `HH:mm:ss` regression scan: **PASS**
- Merge-marker / duplicate-import scan: **PASS**

## Glyph core validation

The Phone (4a) Pro renderer uses a **13×13 / 169-cell** matrix.

Current live expression set:

- Center
- Look left
- Look right
- Blink
- Wink
- Happy
- Hearts
- Squint
- Sleepy

Curious, Playful, Surprised, Side-eye, Excited and Rolling remain harmless legacy enum values and render back to Center if encountered.

Every non-center live animation follows:

```text
CENTER → animation → CENTER → next animation
```

The Center recovery is now about **700 ms** for Look left/right, Happy, Wink, Hearts, Squint, Sleepy and Blink. Due blink/motion timers are pushed beyond that window so another animation cannot begin on the same renderer tick.

The Settings preview now mirrors the same `CENTER → expression → CENTER` contract. It uses a single circular black surface and draws only illuminated cells, eliminating the old visible square grid inside the circle.

The Glyph runtime suppresses identical consecutive frames to reduce visible twitching.

## Focus behavior

Focus remains the only automatic Dayline overlay on the live Glyph face.

- Eyes remain active during Focus and Rest.
- 25/5, 50/10 and custom focus cycles are supported.
- A faint circular progress perimeter remains visible around the face.
- Completed Focus pixels brighten clockwise.
- Break progress runs in reverse.

## Brightness behavior

Dayline Settings keeps a simple 0–100% brightness UI. The live Glyph Toy maps that percentage to the raw `IntArray` Matrix range used by Nothing's official example project, allowing raw output up to **2047** internally. Quiet-hours dimming is applied after this mapping.

## Update behavior

The updater no longer trusts the release tag alone. A candidate must satisfy both:

```text
candidate semantic version > installed semantic version
candidate derived beta versionCode > BuildConfig.VERSION_CODE
```

The downloaded APK is still independently inspected before Android installation, including package name and embedded `versionCode` checks.

## Nothing Phone (4a) Pro integration

The beta flavor registers `DaylineGlyphToyService` as an AOD-capable Glyph Toy and targets the documented Phone (4a) Pro 13×13 matrix. The hardware bridge uses reflection so the open-source tree and Play flavor remain buildable without committing Nothing's proprietary AAR.

Nothing's documentation names `Glyph.DEVICE_25111p`; Dayline first attempts that field and falls back to the documented `25111p` registration target string if needed.

## Android compile gate

Before tagging `v0.14.5.beta`, GitHub Actions must pass:

```text
:app:assembleBetaDebug
:app:assemblePlayDebug
```

For tag `v0.14.5.beta`, the tagged job additionally builds `:app:assembleBetaRelease`, verifies tag/APK version identity, verifies the signature, generates the SHA-256 checksum and publishes the GitHub prerelease.
