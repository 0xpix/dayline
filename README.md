# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is local-first, deliberately minimal, and designed around **Today** instead of a dashboard.

Current milestone: **v0.14.6.beta · Focus timer Glyph**

## What v0.14.6.beta adds

- Replaced the crowded circular Focus progress ring with a dedicated **top eyes + bottom time** layout on the Nothing Phone (4a) Pro 13×13 Glyph Matrix.
- Normal idle mode keeps the existing large expressive 5×5 eyes unchanged.
- During Focus/Rest, Dayline switches to smaller expressive eyes in the top rows and reserves the bottom five rows for a stable `MM:SS` countdown.
- The timer and eye zones never overlap; rows between them are intentionally left empty for visual separation.
- Focus mode reuses the same animation state machine as idle mode: `CENTER → expression → CENTER → next expression`.
- Look Left and Look Right use the same directional behavior as normal mode by shifting the complete compact eye pair left/right.
- Blink, Wink, Happy, Hearts, Squint and rare Sleepy remain available above the timer.
- 25/5, 50/10 and custom cycles count down the current Focus or Rest phase directly instead of converting time into a ring.
- Paused focus sessions keep showing their persisted remaining time.
- The countdown updates independently while duplicate-frame suppression prevents unnecessary hardware refreshes.
- Bumped beta versionCode to **1406** and beta versionName to `0.14.6.beta`.

The v0.14.5 updater and preview protections remain intact: versionCode-aware update discovery, tag/APK release validation, clean circular Settings preview, centered expression transitions and clearer release notes.

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

GitHub Actions compiles both debug flavors on `main`. Beta tags such as `v0.14.6.beta` additionally produce a persistently signed beta APK plus SHA-256 checksum and publish them as a GitHub prerelease.

Before publication, the tagged workflow verifies that the signed APK's embedded version matches the tag and that its Android `versionCode` follows Dayline's beta version convention.

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
