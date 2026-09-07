# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is local-first, deliberately minimal, and designed around **Today** instead of a dashboard.

Current milestone: **v0.14.7.beta · Focus time announcements**

## What v0.14.7.beta adds

- Removed the persistent Focus timer layout from the Glyph Matrix; normal Focus and Rest visuals now use the same large expressive eyes as idle mode.
- Focus time is shown only as a temporary **30-second `MM:SS` announcement**, replacing the eyes completely while it is visible.
- Added the clean transition contract `eyes → CENTER → time → CENTER → eyes`, so a checkpoint never cuts directly from Happy/Left/Wink/etc. into the timer.
- Option B checkpoint schedule is now used for every Focus and Rest phase: show the phase **start**, then every **5-minute remaining checkpoint**, then **1:00 remaining**.
- Examples: 25/5 Focus announces 25:00, 20:00, 15:00, 10:00, 05:00 and 01:00; its Rest phase announces 05:00 and 01:00.
- 50/10 and custom cycles follow the same rule automatically.
- During each 30-second announcement the displayed time remains live and continues counting down; paused Focus keeps the persisted remaining value frozen.
- After the 30-second time window, the Glyph returns through Center and restarts the normal expressive-eye animation cadence.
- Bumped beta versionCode to **1407** and beta versionName to `0.14.7.beta`.

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

GitHub Actions compiles both debug flavors on `main`. Beta tags such as `v0.14.7.beta` additionally produce a persistently signed beta APK plus SHA-256 checksum and publish them as a GitHub prerelease.

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
