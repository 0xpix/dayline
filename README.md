# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is local-first, deliberately minimal, and designed around **Today** instead of a dashboard.

Current milestone: **v0.14.3.beta · Glyph Eyes + Focus polish**

## What v0.14.3.beta adds

Dayline Glyph turns the Nothing Phone (4a) Pro's 13×13 Glyph Matrix into an eyes-first companion for the calendar.

- The Glyph now stays focused on **expressive eyes + Focus Mode**. Automatic calendar/app-state symbols no longer interrupt the face.
- Large solid dot-matrix eyes with **Center, Look left/right, Blink, Wink, Happy, Hearts, Squint and Sleepy**.
- **Every non-center animation returns through Center** before another animation starts: `CENTER → expression → CENTER → next expression`.
- More frequent natural blinking, more Happy, and much rarer Sleepy behavior.
- Removed the weaker Curious, Playful, Surprised, Side-eye, Excited and Rolling expressions from the live loop and Settings preview.
- Reworked Hearts into a smaller, cleaner heart-eye pattern.
- Focus cycles keep the eyes alive while a **circular pixel progress path** fills around them for **25/5, 50/10 and custom** focus cycles.
- Focus progresses clockwise; break progresses in reverse.
- The focus perimeter stays faintly visible so early progress reads as a circle instead of a stray line.
- Glyph brightness maps Dayline's simple 0–100% control to the higher raw intensity range used by Nothing's official `IntArray` Matrix examples.
- Stable frame delivery suppresses duplicate frames to reduce visible twitching/flicker.
- Simplified Glyph settings: enable, hardware, brightness, blink, expressions/glances, motion frequency, Focus behavior, quiet hours and a compact expression preview.
- A Phone (4a) Pro AOD Glyph Toy service remains available in the GitHub beta flavor, with a shortcut to Nothing's Glyph Toys manager.

The larger v0.12/v0.13 feature set remains intact: calendar controls, recurrence scopes, timeline manipulation, focus cycles, tasks, templates, backup/ICS, widget configuration, GitHub beta updates and Play separation.

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

GitHub Actions compiles both debug flavors on `main`. Beta tags such as `v0.14.3.beta` additionally produce a persistently signed beta APK plus SHA-256 checksum and publish them as a GitHub prerelease.

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
