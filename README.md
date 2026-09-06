# Dayline

**A quieter way to plan your day.**

Dayline is an open-source Android calendar and planner built with Kotlin, Jetpack Compose, Material 3 and Glance. It is intentionally local-first, minimal, and designed around Today rather than a dashboard.

Current milestone: **v0.12.7 · Play beta**


## v0.12.7 emoji picker + notification + typography polish

- Widget emoji chooser is now a vertically scrollable monochrome **Noto Emoji**
  grid rendered by the same renderer as the actual widget.
- Removed the yellow AndroidX/system emoji picker.
- The picker covers broad Unicode emoji/symbol blocks, keycaps and flags and
  filters candidates by Noto glyph availability.
- Notification progress uses seconds internally, starts visibly immediately,
  and refreshes on the next minute boundary instead of waiting 5–15 minutes.
- Every Material 3 typography role is now bound to the selected Dayline font,
  fixing mixed Pixelify/system text in Quick Add.
- The floating Today/Home button now reproduces the Dayline launcher-logo shape.

## v0.12.6 repeat-day chooser

- Simplified visible Repeat choices to **Once, Daily, Weekdays, Weekend,
  Choose days**.
- **Choose days** opens a seven-day checkbox dialog.
- Added `repeatDays` persistence and Android Calendar / ICS BYDAY support.
- Migrates the short-lived v0.12.5 `SUNDAYS` and `EXCEPT_SUNDAY` values.
- Fixed the malformed apostrophe glyph in `DotMatrixRenderer.kt`.
- Fixed the non-exhaustive `DaylineTransfer.kt` recurrence build failure.
- Improved Quick Add heading and section spacing.

## v0.12.5 Unicode widget + recurrence polish

- Widget event titles no longer turn unsupported characters into `?`.
  Ampersand is supported by the dot renderer; emoji and other Unicode use
  native Glance text as a safe fallback.
- Unicode title truncation is code-point safe, so emoji surrogate pairs are not cut.
- Event-name pills have a dedicated system-neutral contrast surface and remain
  visible in light mode.
- Pulse layout now uses symmetric emoji padding, divider spacing, and information
  padding: emoji · `|` · information.
- Repeat adds **Sunday only** and **Every day except Sunday**, including Calendar
  Provider RRULE and ICS import/export support.
- Quick Add has a clearer padded title field, stronger section hierarchy, and
  more breathing room between labels and controls.

## v0.12.4 settings, sync and upcoming polish

- Removed widget controls from the in-app Settings screen. Widget options now
  live only in each widget's Android configuration screen.
- Widget configuration rows now open real chooser sheets instead of cycling values.
- Emoji selection uses AndroidX EmojiPickerView with the complete Emoji 16.0
  picker/categories/variants; the chosen glyph is rendered with Dayline's Noto Emoji path.
- "Slide long titles" is now stored per widget.
- Widget backgrounds use Android's dynamic neutral system palette on Android 12+
  with neutral fallbacks on older Android versions.
- Synced Dayline events are reconciled with Calendar Provider deletion, so deleting
  a mapped event from Google Calendar/another calendar removes it from Dayline.
- App font list is cleaned up: System, Geist, Inter, Space Grotesk, IBM Plex Mono,
  and Pixelify Sans. Legacy Geist Pixel migrates visually to Geist.
- Quick Add now uses wrapping, roomier controls and common templates:
  Meeting, Focus block, Workout, Appointment and Errand.
- Upcoming now supports All, Meetings, Holidays, Events, Tasks, individual
  calendar, and Space filters.

## v0.12.3 interaction + layout polish

- Fixed drag-to-move and drag-to-resize commit behavior. Gesture end now uses
  gesture-local snapped values instead of stale recomposition-derived values.
- Fixed the same stale-value issue for dragging unscheduled tasks onto the timeline.
- Undo snackbars now dismiss automatically after the normal short duration.
- Today, Tasks, Spaces, Settings and Task detail use lower Dawn-like vertical staging.
  Calendar, Upcoming and Search retain their denser top layout.
- Refined the floating right-side controls and navigation sheet.
- Widget configuration now includes a live preview that reacts to emoji, font,
  Space/calendar filters, events/tasks and content mode.
- Pulse uses more of its right edge and removes the awkward NEXT/NOW tail.
- Now notifications show the event end first and include a clear START → END range.

## v0.12.2 build repair

- Fixed the Today empty-gap height calculation in `DayTimeline.kt`.
- `Duration.toMinutes()` returns `Long`; the value is now converted to `Int`
  before applying Compose's `.dp` extension.
- No v0.12.1 widget/Noto Emoji behavior was changed.

## v0.12.1 widget polish

- Widget emoji artwork is no longer stored as custom PNG files.
- Dayline requests **Google Fonts · Noto Emoji** through Android's downloadable-font provider and renders those glyphs for Glance.
- No font binary is bundled in the repository or APK source tree.
- The Pulse widget left side now contains only the emoji glyph; the `DAYLINE` caption was removed.
- Pulse, Orbit and Lock widgets now use one full neutral system surface with `onSurface` / `onSurfaceVariant` text colors.
- The old transparent-widget switch was removed to avoid wallpaper-dependent contrast problems.

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
