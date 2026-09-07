# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is local-first, deliberately minimal, and designed around **Today** instead of a dashboard.

Current milestone: **v0.17.0.beta · Flow**

## What v0.17.0.beta adds

### Added

- **Free gaps directly on Today**, with quick actions to add an event, schedule a task or start a Focus block.
- A dedicated **ALL DAY** strip above the timed timeline.
- Task scheduling windows with optional **Earliest date**, **Deadline** and persistent duration estimates.
- **Quick Move v2** with before/after-block, tomorrow morning, tomorrow afternoon and next-free-slot choices.
- Conflict resolution actions that show overlap duration and offer a real free slot instead of only warning about the clash.
- Local Search commands for **this week**, **next week**, weekdays and queries such as `free Friday afternoon`.
- Expanded hidden beta diagnostics with widget refresh time, next reminder, Focus state and local Glyph transport counters.
- Recurrence/planning unit tests in CI, including monthly 31st behavior, exclusions, all-day planning and deadline-aware task fitting.

### Changed

- Today drag/resize now snaps in **5-minute** steps, gives a stronger beat every 15 minutes, uses a much larger invisible resize target and shows a floating move/end-time preview.
- Today opens near the useful part of the day, fades past events gently and auto-scrolls while an event is dragged near the timeline edge.
- Month view has clearer selection, calmer weekend treatment and a compact busy-day overflow mark.
- Day Preview separates **ALL DAY / AGENDA / FREE TIME** and adds a clear **OPEN DAY** action.
- Fit into my day now respects a task's earliest/deadline window instead of always searching an unconditional seven days.
- The beta updater now preserves the literal **Added / Changed / Fixed** headings from GitHub release notes and records APK size metadata.
- Glyph reliability behavior itself is unchanged; v0.17 only instruments successful frames, disconnects, recoveries and send failures for diagnosis.

### Fixed

- Fixed the real updater parsing path that was stripping Markdown section headings before the UI could render Added / Changed / Fixed as separate cards.
- Monthly recurrence on the 29th/30th/31st no longer silently shifts into a shorter month's last day.
- All-day entries no longer consume timed free-space calculations or create false overlap conflicts.
- Beta identity is **1700 / `0.17.0.beta`**.

The v0.16 Planning milestone remains intact: Month view, Day preview, free-time detection, task duration, Fit into my day, event details, Quick Move, cleaner Upcoming, swipe Today ↔ Upcoming and beta diagnostics.

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

The tagged release workflow refuses to publish a beta without those sections. The beta updater preserves those headings and the app renders them as three visually separate cards.

## Privacy

Dayline has no Dayline account, advertising SDK, analytics SDK or Dayline-operated cloud. Calendar access is optional and uses Android's Calendar Provider. GitHub beta update checks send an ordinary HTTPS request to GitHub's public release endpoint; Dayline calendar/task/focus data is not included. Beta diagnostics and Glyph counters stay on-device unless you explicitly share the generated text report. See [Privacy Policy](docs/privacy-policy.md) and [Data Safety Notes](docs/data-safety.md).

## Build from source

Requirements: JDK 17, Gradle 9.4.1, Android SDK API 37 and Build Tools 36.0.0.

```bash
gradle :app:testBetaDebugUnitTest :app:testPlayDebugUnitTest :app:assembleBetaDebug :app:assemblePlayDebug --no-daemon
```

GitHub Actions runs planning/recurrence tests and compiles both debug flavors on `main`. Beta tags such as `v0.17.0.beta` additionally produce a persistently signed beta APK plus SHA-256 checksum and publish them as a GitHub prerelease.

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
