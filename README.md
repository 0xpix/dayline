# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is intentionally local-first, minimal, and designed around Today rather than a dashboard.

Current milestone: **v0.12.0 · Play beta**

## What v0.12 adds

This milestone brings the product-polish pass together:

1. Per-calendar visibility, default calendar, editability, color and Space mapping.
2. Recurring edit scope: occurrence / following / entire series.
3. Long-press drag to move timed blocks and a bottom handle to resize duration.
4. Tap empty timeline time to create an event at that time.
5. Focus modes: Off, 25/5, 50/10 and custom focus/rest durations with session stats.
6. Ongoing focus controls: pause/resume, skip rest, +5 minutes and finish.
7. Live remaining-time/event progress; focus blocks use phase progress instead.
8. Minimal Today BUSY / OPEN / BLOCKS summary.
9. Overlap/conflict indicators.
10. Before/after travel-buffer reservations.
11. Event templates, including editable starter templates.
12. Spaces can route new events to a selected Android calendar.
13. Tasks add due time, recurrence, priority, subtasks, Task→Event and drag-to-schedule.
14. Undo for destructive/move/resize scheduling actions.
15. Minimal Search / command-style filtering.
16. JSON backup/restore plus ICS export/import.
17. Per-widget Space/calendar/emoji/font/content/background/task/event/focus configuration.
18. Widgets switch between NEXT, active-event remaining time, FOCUS and REST states.
19. Subtle haptics on direct manipulation, completion and creation interactions.
20. A three-step first-run setup for calendar access, calendar choices and appearance.

## Privacy

Dayline has no Dayline account, advertising SDK, analytics SDK or Dayline-operated cloud. Calendar access is optional and uses Android's Calendar Provider. See [Privacy Policy](docs/privacy-policy.md) and [Data Safety Notes](docs/data-safety.md).

## Build from source

Requirements: JDK 17, Gradle 9.4.1, Android SDK API 37 and Build Tools 36.0.0.

```bash
gradle :app:assembleDebug --no-daemon
```

GitHub Actions continues to build the debug APK on `main` and on version tags.

## Google Play build

Dayline keeps GitHub distribution and Play distribution separate:

- `build-apk.yml` → open-source/debug GitHub APK workflow
- `play-release.yml` → manually triggered, signed release APK + Android App Bundle (`.aab`)

The Play workflow reads the upload key only from GitHub Actions secrets. Signing material is never stored in the repository. See [Play Store Checklist](docs/play-store-checklist.md).

## Open source

Dayline is licensed under the [MIT License](LICENSE). Contributions are welcome; see [CONTRIBUTING.md](CONTRIBUTING.md).


### Now activity
Active events use a clean ongoing notification with compact remaining time (for example `42M LEFT`) and a progress bar. Focus cycles show `FOCUS` / `REST` plus the session count. Dayline intentionally avoids the system HH:MM:SS chronometer presentation.
