# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is local-first, deliberately minimal, and designed around **Today** instead of a dashboard.

Current milestone: **v0.14.4.beta · Centered Glyph transitions**

## What v0.14.4.beta adds

Dayline Glyph keeps the Nothing Phone (4a) Pro's 13×13 Glyph Matrix calm and readable by forcing every expression to transition through the Center face.

- **Every non-center animation is isolated by Center**: `CENTER → animation → CENTER → next animation`.
- The rule applies to **Look left, Look right, Blink, Wink, Happy, Hearts, Squint and Sleepy**.
- A dedicated ~500 ms Center recovery prevents messy sequences such as `Sleepy → Happy → Right`.
- Due blink/motion timers are pushed beyond the Center recovery window so the next animation cannot start on the same renderer tick.
- The approved expression set remains **Center, Look left/right, Blink, Wink, Happy, Hearts, Squint and rare Sleepy**.
- Focus Mode remains the only automatic Dayline overlay, with the circular focus/rest progress perimeter around the eyes.
- Glyph brightness still maps Dayline's 0–100% control to the higher raw `IntArray` Matrix intensity range used by Nothing's official examples.
- Duplicate raw frames remain suppressed to reduce visible twitching/flicker.

The larger v0.14.3 Glyph polish remains intact: trimmed expression set, improved hearts, brighter raw output, circular Focus progress, simplified Glyph settings and stable frame delivery.

The larger v0.12/v0.13 feature set also remains intact: calendar controls, recurrence scopes, timeline manipulation, focus cycles, tasks, templates, backup/ICS, widget configuration, GitHub beta updates and Play separation.

## Nothing Glyph Matrix distribution

The open-source repository does **not** contain Nothing's proprietary Glyph Matrix AAR. The GitHub beta workflow verifies that the binary is absent from source, then fetches `glyph-matrix-sdk-2.0.aar` from Nothing's official developer repository for the beta build. The `play` flavor deliberately does not include this SDK while commercial licensing is unresolved. See [Glyph Matrix integration](docs/glyph-matrix.md).

On Phone (4a) Pro, after installing the beta:

1. Open Dayline → **Settings → Glyph → Dayline Glyph**.
2. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
3. Tap **ACTIVATE IN NOTHING SETTINGS**.
4. Select **Dayline Eyes** under **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy**.

## Distribution channels

- **Dayline β (`com.pix.dayline.beta`)** — GitHub beta APK with the public GitHub updater and Nothing Glyph Matrix integration.
- **Dayline (`com.pix.dayline`)** — Play flavor with no GitHub updater permissions and no proprietary Glyph Matrix SDK in this beta milestone.

Only the beta flavor requests `INTERNET`, `REQUEST_INSTALL_PACKAGES`, and Nothing's `com.nothing.ketchum.permission.ENABLE`. The Play flavor does not receive those permissions.

Tagged GitHub beta APKs must keep using the same persistent beta signing key. Android will reject an in-place update if a later APK is signed with a different key. See [GitHub beta updates](docs/github-beta-updates.md) and [GitHub beta signing](docs/github-beta-signing.md).

> If an older beta was signed with a different temporary/debug key, Android may require one uninstall/reinstall when moving to the persistent beta key. Back up Dayline first. After that baseline is installed, future signed betas can update in place with the same key.

## Privacy

Dayline has no Dayline account, advertising SDK, analytics SDK or Dayline-operated cloud. Calendar access is optional and uses Android's Calendar Provider. GitHub beta update checks send an ordinary HTTPS request to GitHub's public release endpoint; Dayline calendar/task/focus data is not included. See [Privacy Policy](docs/privacy-policy.md) and [Data Safety Notes](docs/data-safety.md).

## Build from source

Requirements: JDK 17, Gradle 9.4.1, Android SDK API 37 and Build Tools 36.0.0.

```bash
gradle :app:assembleBetaDebug :app:assemblePlayDebug --no-daemon
```

GitHub Actions compiles both debug flavors on `main`. Beta tags such as `v0.14.4.beta` additionally produce a persistently signed beta APK plus SHA-256 checksum and publish them as a GitHub prerelease.

## Google Play build

Dayline keeps GitHub distribution and Play distribution separate:

- `.github/workflows/build-apk.yml` → GitHub beta CI + signed prerelease APK workflow
- `.github/workflows/play-release.yml` → manually triggered signed `playRelease` APK + Android App Bundle (`.aab`)

The workflows read signing material only from GitHub Actions secrets. Signing keys and passwords are never stored in the repository. See [Play Store Checklist](docs/play-store-checklist.md).

## Validate

Run the static project validator before pushing:

```bash
python3 scripts/validate_release.py
```

The authoritative Android/Compose compile remains GitHub Actions (or a local Android SDK build).

## Open source

Dayline is licensed under the [MIT License](LICENSE). Contributions are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md).
