<div align="center">

<img src="docs/assets/dayline-mark.svg" width="112" alt="Dayline app icon" />

# Dayline

### A quieter way to plan your day.

**Dayline is a minimal, local-first calendar and daily planner for Android.**  
It brings events, tasks, free time, Focus, widgets and Android Calendar sync into one calm Today-first timeline.

[![Beta](https://img.shields.io/badge/status-beta-111111?style=flat-square)](https://github.com/0xpix/dayline/releases/tag/v0.17.0.beta)
[![Release](https://img.shields.io/badge/version-v0.17.0.beta-111111?style=flat-square)](https://github.com/0xpix/dayline/releases/tag/v0.17.0.beta)
[![Android](https://img.shields.io/badge/Android-13%2B-111111?style=flat-square&logo=android)](#install-the-beta)
[![Kotlin](https://img.shields.io/badge/Kotlin-Compose-111111?style=flat-square&logo=kotlin)](#build-from-source)
[![License](https://img.shields.io/badge/license-MIT-111111?style=flat-square)](LICENSE)

[**Download the latest beta**](https://github.com/0xpix/dayline/releases/tag/v0.17.0.beta) · [Report a bug](https://github.com/0xpix/dayline/issues) · [Privacy](docs/privacy-policy.md)

</div>

---

## What is Dayline?

Dayline is built around a simple idea: **your calendar should show the shape of your day, not bury it inside a dashboard.**

The main screen is a timeline. Your events and scheduled tasks sit where they actually happen. Open gaps remain visible, so you can immediately see when you are busy, when you are free, and where another task can realistically fit.

Dayline is designed to stay quiet and useful:

- **Today-first** instead of dashboard-first.
- **Local-first** with no Dayline account or cloud required.
- **Minimal** instead of filling the screen with productivity scores and widgets.
- **Deterministic** planning instead of opaque AI scheduling.
- **Android-native**, including Calendar Provider sync, reminders, widgets and optional Nothing Glyph Matrix support.

## Why the name “Dayline”?

**Dayline = day + timeline.**

The name describes the central idea of the app: turning a day into one clear line of time. Events, tasks, Focus blocks and free gaps all live on that same line instead of being split across unrelated screens.

## The icon

<img src="docs/assets/dayline-mark.svg" width="88" align="left" alt="Dayline icon" />

The Dayline icon is a **segmented square dial** surrounding a horizontal line and marker.

The outer segments suggest the frame of a day or clock face. The line in the middle represents the Dayline timeline, while the small block at its end acts as the current/planned point on that line.

It is intentionally geometric and monochrome so it remains readable as an Android adaptive icon and as a themed monochrome icon on modern Android launchers.

<br clear="left" />

## Current version

| | |
|---|---|
| **Release** | `v0.17.0.beta` |
| **Android versionCode** | `1700` |
| **Status** | Public beta |
| **Beta package** | `com.pix.dayline.beta` |
| **GitHub beta minimum Android** | Android 13 / API 33 |
| **Stable Play package** | `com.pix.dayline` |
| **Source license** | MIT |

The current GitHub beta requires Android 13+ because the beta build includes Nothing's Glyph Matrix SDK. The Play flavor keeps Dayline's lower global Android requirement and does not package the proprietary Nothing SDK.

## What Dayline does

### Today

The Today screen is the center of the app.

- Events and scheduled tasks appear directly on a vertical timeline.
- A current-time marker shows where you are in the day.
- Past items fade quietly so upcoming time remains prominent.
- **FREE** blocks show usable gaps between commitments.
- Tap a free block to create an event, schedule a task or start Focus.
- Drag events to move them.
- Resize them from the bottom edge.
- Move/resize snaps in 5-minute steps with stronger 15-minute haptic landmarks.
- All-day items stay in their own strip instead of pretending to occupy a time slot.

### Planning

Dayline includes a small local planning engine rather than an AI planner.

- Detects free time from your real schedule.
- Understands event buffers.
- Respects task duration, earliest date and deadline.
- **Fit into my day** suggests real open slots.
- **Quick Move** can move something before/after another block, tomorrow morning/afternoon or to the next free slot.
- Conflict handling shows how much two items overlap and offers useful alternatives.

### Tasks

Tasks can stay lightweight or become part of the timeline.

- Unscheduled or scheduled tasks.
- Duration estimates.
- Priority.
- Earliest date and deadline.
- Completion by occurrence for recurring tasks.
- Fit tasks into available time without leaving the app.

### Calendar

Dayline works with Android's Calendar Provider.

- Optional Android Calendar sync.
- Multiple calendars.
- Per-calendar visibility and write behavior.
- External deletions and edits reconcile back into Dayline.
- Recurring events and occurrence exceptions are supported.
- Timed events preserve their provider timezone through Dayline edits.
- All-day events remain all-day.

### Month, Upcoming and Search

- Month view with compact event/task indicators.
- Day Preview with **ALL DAY / AGENDA / FREE TIME**.
- Upcoming grouped by date with compact filtering.
- Local Search for text, dates, weekdays, tasks/events and commands such as `this week`, `next week` or `free Friday afternoon`.

### Focus

Events can carry Focus cycles, including:

- 25 / 5
- 50 / 10
- custom Focus / Rest timing

Focus can also be started directly from a free block.

### Widgets

Dayline includes minimalist home-screen widgets designed to stay visually close to the app:

- current / next event information
- task state
- Focus / Rest state
- system-aware light and dark appearance
- per-widget font and emoji configuration
- refresh after Dayline changes, reboot, app replacement and system date/time/timezone changes

### Nothing Glyph Matrix

The GitHub beta includes optional support for the **Nothing Phone (4a) Pro 13×13 Glyph Matrix**.

Dayline Eyes can show expressive idle animations such as Center, Look Left, Look Right, Blink, Wink, Happy, Hearts, Squint and Sleepy. During Focus, the Glyph can temporarily show the remaining time before returning to the eyes.

Glyph support is deliberately optional and does not change normal calendar behavior.

The proprietary Nothing SDK is **not committed to this repository**. GitHub Actions downloads it from Nothing's official developer repository only for the beta build. The Play flavor does not include it while commercial licensing is unresolved.

See [Glyph Matrix integration](docs/glyph-matrix.md).

## A simple Dayline day

```text
04:30   GYM
        ─────────────
09:00   FREE · 1H
        ─────────────
10:00   LUMEN / PHD
        ─────────────
14:00   FREE · 45M
        ─────────────
14:45   TOEFL
        ─────────────
16:15   DEEP WORK
```

That is the basic philosophy of Dayline: **show the day clearly, then help you use the gaps.**

## Install the beta

The current public release is **v0.17.0.beta**.

1. Open the [v0.17.0.beta release](https://github.com/0xpix/dayline/releases/tag/v0.17.0.beta).
2. Download `dayline-v0.17.0.beta.apk`.
3. Allow installation from your browser/file manager when Android asks.
4. Install Dayline.

The release also includes a `.sha256` checksum. The release workflow verifies the APK version, versionCode and signing certificate before publishing it.

Future GitHub beta updates can be checked and installed from inside the beta app.

## Beta status

Dayline is intentionally still a **beta**.

The current focus is reliability and real-world feedback rather than adding more features. The most important areas being tested are:

- Calendar synchronization
- recurrence and occurrence editing
- reminders
- widgets
- timeline drag/resize behavior
- backup/restore and migrations
- updater reliability
- Nothing Glyph stability

If something behaves incorrectly, please [open an issue](https://github.com/0xpix/dayline/issues). For bugs, include your Dayline version, Android version and device model when possible, but do not post private calendar content.

## Privacy

Dayline has:

- no Dayline account
- no advertising SDK
- no analytics SDK
- no Dayline-operated cloud

Calendar access is optional and uses Android's Calendar Provider. GitHub beta update checks contact GitHub's public release endpoint but do not send your calendar, task or Focus data.

Beta diagnostics remain on-device unless you explicitly share the generated report.

See the [Privacy Policy](docs/privacy-policy.md) and [Data Safety Notes](docs/data-safety.md).

## Distribution

Dayline keeps its two distribution targets separate:

| Channel | Package | Purpose |
|---|---|---|
| **GitHub beta** | `com.pix.dayline.beta` | Public beta, updater, Nothing Glyph Matrix integration |
| **Play** | `com.pix.dayline` | Stable distribution target without the proprietary Glyph Matrix SDK |

The beta flavor alone requests the extra permissions needed for GitHub updates and Nothing Glyph support.

## Build from source

Requirements:

- JDK 17
- Gradle 9.4.1
- Android SDK API 37
- Build Tools 36.0.0

```bash
gradle \
  :app:testBetaDebugUnitTest \
  :app:testPlayDebugUnitTest \
  :app:assembleBetaDebug \
  :app:assemblePlayDebug \
  --no-daemon
```

GitHub Actions also runs static release validation, recurrence/planning tests and both Beta/Play compile gates.

For release/signing details see:

- [GitHub beta updates](docs/github-beta-updates.md)
- [GitHub beta signing](docs/github-beta-signing.md)
- [Play Store checklist](docs/play-store-checklist.md)

## Contributing

Dayline is open source and licensed under the [MIT License](LICENSE).

Bug reports, testing feedback and focused pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) before contributing.

---

<div align="center">

**Dayline** · A quieter way to plan your day.

`v0.17.0.beta`

</div>
