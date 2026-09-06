# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is local-first, deliberately minimal, and designed around **Today** instead of a dashboard.

Current milestone: **v0.13.0.beta · update & reliability beta**

## What v0.13.0.beta adds

This beta is focused on reliability, direct manipulation and making the GitHub beta channel usable before Play testing:

1. A GitHub release updater that reads the public Releases list, including prereleases, instead of relying on `/releases/latest`.
2. A minimal update sheet with version, release notes, verified APK download and Android install flow.
3. Version, versionCode/build, update channel and short Git commit identity in Settings → About.
4. Optional low-impact automatic update checks, at most once per day for GitHub beta builds.
5. Calendar sync health with last successful sync, provider errors and per-calendar SYNCED / HIDDEN / READ ONLY state.
6. Further Now-notification polish: no `HH:mm:ss`, no system chronometer, minute-level refresh, progress, focus/rest state and compact session dots.
7. Undo for move, resize, delete, task scheduling and Task → Event conversion.
8. Tappable overlap markers that explain conflicting blocks in a compact sheet.
9. Command-style Search for `today`, `tomorrow`, `unfinished`, `focus`, tasks/events, month names, titles, Spaces and calendar names; results open the matching occurrence.
10. Today polish: visible date marker, larger resize target, exact drag/resize previews, 15-minute haptic snaps and return-to-now behavior.

It keeps the larger v0.12 feature set: per-calendar controls, recurring edit scopes, timeline move/resize/tap-to-create, 25/5 + 50/10 + custom focus cycles, buffers, templates, richer tasks, backup/ICS, per-widget configuration, smart widget states, haptics and first-run setup.

## Distribution channels

Dayline deliberately separates GitHub and Play behavior:

- **Dayline β (`com.pix.dayline.beta`)** — open-source GitHub beta APK with the GitHub Releases updater, APK verification and Android's normal install confirmation flow.
- **Dayline (`com.pix.dayline`)** — Play flavor with no GitHub-updater permissions; stable updates are delivered by Google Play.

Only the beta flavor requests `INTERNET` and `REQUEST_INSTALL_PACKAGES` for the self-update flow. The Play flavor does not receive those permissions.

Tagged GitHub beta APKs must keep using the same persistent beta signing key. Android will reject an in-place update if a later APK is signed with a different key. See [GitHub beta updates](docs/github-beta-updates.md) and [GitHub beta signing](docs/github-beta-signing.md).

> If an older beta was signed with a different temporary/debug key, Android may require one uninstall/reinstall when moving to the persistent beta key. Back up Dayline first. After that baseline is installed, future signed betas can update in place with the same key.

## Privacy

Dayline has no Dayline account, advertising SDK, analytics SDK or Dayline-operated cloud. Calendar access is optional and uses Android's Calendar Provider. GitHub beta update checks send an ordinary HTTPS request to GitHub's public release endpoint; Dayline calendar/task/focus data is not included. See [Privacy Policy](docs/privacy-policy.md) and [Data Safety Notes](docs/data-safety.md).

## Build from source

Requirements: JDK 17, Gradle 9.4.1, Android SDK API 37 and Build Tools 36.0.0.

```bash
gradle :app:assembleBetaDebug :app:assemblePlayDebug --no-daemon
```

GitHub Actions compiles both debug flavors on `main`. Beta tags such as `v0.13.0.beta` additionally produce a persistently signed beta APK plus SHA-256 checksum and publish them as a GitHub prerelease.

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
