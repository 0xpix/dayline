# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is local-first, deliberately minimal, and designed around **Today** instead of a dashboard.

Current milestone: **v0.16.0.beta · Planning**

## What v0.16.0.beta adds

### Added

- A redesigned **Month view** with compact multi-event day indicators and a tap-to-open **Day preview**.
- Local **free-time detection** in day previews. Dayline merges busy blocks and shows useful gaps without sending calendar data anywhere.
- Persistent **task duration estimates** and **Fit into my day** suggestions that place a task into matching free blocks over the next seven days.
- A calmer **event detail sheet** with date/time/duration metadata and **Quick Move** actions for Later today, Tomorrow and Next free slot.
- Hidden **Beta diagnostics**: in a GitHub beta, tap **Settings → About → Build** five times to inspect build, updater, Calendar sync and Glyph hardware status.
- Gesture navigation between the two daily-flow screens: **swipe left on Today → Upcoming**, then **swipe right on Upcoming → Today**.

### Changed

- Today ↔ Upcoming now uses a directional horizontal slide/fade animation so the navigation follows the swipe.
- Upcoming keeps its date groups but moves the crowded permanent filter chips into one compact **Filter** sheet.
- Month view is planning-first: tap a date to see events/tasks plus free blocks instead of permanently expanding an agenda below the grid.
- Task scheduling uses a deterministic local planning engine with 15-minute alignment, buffers and overlap merging.

### Fixed

- Quick Move on recurring events detaches only the selected occurrence instead of silently shifting the master series.
- Scheduled tasks reserve their estimated duration when Dayline calculates free time and conflicts.
- Beta version is **1600 / `0.16.0.beta`**.

The v0.15 reliability work remains intact, including two-way Android Calendar reconciliation, recurrence scopes, Today resize behavior, updater presentation and conservative Glyph recovery.

## Nothing Glyph Matrix distribution

The open-source repository does **not** contain Nothing's proprietary Glyph Matrix AAR. The GitHub beta workflow verifies that the binary is absent from source, then fetches `glyph-matrix-sdk-2.0.aar` from Nothing's official developer repository for the beta build. The `play` flavor deliberately does not include this SDK while commercial licensing is unresolved. See [Glyph Matrix integration](docs/glyph-matrix.md).

On Phone (4a) Pro, after installing the beta:

1. Open Dayline → **Settings → Glyph → Dayline Glyph**.
2. Enable **Dayline Glyph** and adjust brightness/animation behavior if wanted.
3. Tap **OPEN NOTHING SETTINGS**.
4. Select **Dayline Eyes** under **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy**.

## Distribution channels

- **Dayline β (`com.pix.dayline.beta`)** — GitHub beta APK with the public GitHub updater and Nothing Glyph Matrix integration.
- **Dayline (`com.pix.dayline`)** — Play flavor with no GitHub updater permissions and no proprietary Glyph Matrix SDK in this beta milestone.

Only the beta flavor requests `INTERNET`, `REQUEST_INSTALL_PACKAGES`, and Nothing's `com.nothing.ketchum.permission.ENABLE`. The Play flavor does not receive those permissions.

Tagged GitHub beta APKs must keep using the same persistent beta signing key. Android will reject an in-place update if a later APK is signed with a different key. See [GitHub beta updates](docs/github-beta-updates.md) and [GitHub beta signing](docs/github-beta-signing.md).

> If an older beta was signed with a different temporary/debug key, Android may require one uninstall/reinstall when moving to the persistent beta key. Back up Dayline first. After that baseline is installed, future signed betas can update in place with the same key.

## Release notes

Each beta release has a short updater-facing file at:

```text
docs/releases/<tag>.md
```

It must contain:

```text
## Added
## Changed
## Fixed
```

The tagged release workflow refuses to publish a beta without those sections. The app parses those headings into clean in-app groups, so the **Dayline update** sheet never has to display raw release Markdown.

## Privacy

Dayline has no Dayline account, advertising SDK, analytics SDK or Dayline-operated cloud. Calendar access is optional and uses Android's Calendar Provider. GitHub beta update checks send an ordinary HTTPS request to GitHub's public release endpoint; Dayline calendar/task/focus data is not included. See [Privacy Policy](docs/privacy-policy.md) and [Data Safety Notes](docs/data-safety.md).

## Build from source

Requirements: JDK 17, Gradle 9.4.1, Android SDK API 37 and Build Tools 36.0.0.

```bash
gradle :app:assembleBetaDebug :app:assemblePlayDebug --no-daemon
```

GitHub Actions compiles both debug flavors on `main`. Beta tags such as `v0.16.0.beta` additionally produce a persistently signed beta APK plus SHA-256 checksum and publish them as a GitHub prerelease.

Before publication, the tagged workflow verifies that the signed APK's embedded version matches the tag, that its Android `versionCode` follows Dayline's beta version convention, and that the updater-facing release notes contain Added / Changed / Fixed sections.

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
