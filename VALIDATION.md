# Dayline v0.16.0.beta — Validation Report

This report tracks the current **v0.16.0.beta / versionCode 1600** source. GitHub Actions remains the authoritative Android/Compose compile gate.

## Release checks

- Beta target: **0.16.0.beta / 1600**.
- `docs/releases/v0.16.0.beta.md` contains concise **Added / Changed / Fixed** updater notes.
- Beta/Play flavor separation remains intact; Play stays on the stable base version and does not package the Nothing SDK.
- Tagged builds must verify signed APK versionName/versionCode against tag `v0.16.0.beta`.
- Static validation, Beta debug compile and Play debug compile must all pass before tagging.

## Planning engine

`PlanningEngine` is deterministic and local-only.

- Free-time calculation considers only occurrences on the requested date.
- Busy ranges are merged before gaps are emitted.
- Event/task buffers extend busy intervals.
- Un-timed tasks do not consume a time interval.
- Scheduled tasks without an explicit end reserve `estimatedDurationMinutes`.
- Fit suggestions align starts to 15-minute boundaries and search at most seven days by default.
- No calendar/event/task payload is sent to a remote planning service.

## Month + day preview

- Calendar opens as a clean month grid.
- Each populated day shows up to three compact item indicators instead of large event blocks.
- Today has a distinct monochrome treatment.
- Tapping a date opens a Day preview sheet.
- Day preview shows event/task counts, ordered agenda rows and up to four free-time windows.
- Tasks can still be completed from the preview and items open the exact date occurrence.

## Task duration + Fit into my day

- `DaylineItem.estimatedDurationMinutes` defaults to 30 minutes and is persisted in Dayline JSON.
- Existing backups/items without the field safely load with the default.
- Task Detail exposes an Estimate row cycling 15 / 30 / 45 / 60 / 90 / 120 minutes.
- Changing a scheduled task estimate updates its end time.
- Fit into my day shows the next matching free blocks and schedules the task without converting it into an event.

## Event detail + Quick Move

- Event taps open a compact read-first detail sheet rather than the full editor immediately.
- The sheet shows occurrence date, start/end, duration, Space/calendar and recurrence metadata.
- Edit remains a separate action.
- Quick Move uses real free slots for Later today, Tomorrow and Next free slot.
- One-off events move in place with Undo.
- Recurring Quick Move detaches only the selected occurrence through `SeriesEditor.THIS_OCCURRENCE` and supports Undo.
- Read-only provider events may be viewed but cannot be edited or moved.

## Upcoming cleanup

- Upcoming defaults to the next seven days.
- The permanent WHEN/SHOW chip rows are removed.
- One compact summary opens the Filter sheet.
- Filter sheet preserves Today / Tomorrow / 7 days / All and All / Events / Tasks / Focus / Meetings / Holidays / calendar / Space choices.
- Results remain grouped by Today, Tomorrow or date.
- Task duration is shown as lightweight row metadata.

## Today ↔ Upcoming swipe

- Swipe left on Today transitions to Upcoming.
- Swipe right on Upcoming transitions to Today.
- Swipe transitions use ~300 ms directional horizontal slide plus a light fade.
- Vertical timeline scrolling remains separate from horizontal gesture detection.
- Menu navigation remains available and unchanged.

## Beta diagnostics

- Available only in the GitHub beta channel.
- Tap **Settings → About → Build** five times to open it.
- Reports version/code/commit, update channel/status, Calendar sync status/time and Glyph hardware availability.
- Diagnostics deliberately do not claim a Glyph transport connection state when the SDK does not expose a reliable one.

## Existing v0.15 reliability baseline

- Today body move and bottom-handle resize remain separate.
- Android Calendar mapped events continue two-way reconciliation.
- Recurrence edit scopes remain This occurrence / This + following / Entire series.
- Reminder and Now Activity rescheduling remains enabled across reboot/time/timezone changes.
- Updater uses curated Added / Changed / Fixed release notes with a distinct Download & update button.
- Glyph keeps the conservative lifecycle/recovery strategy and the compact stacked Focus time display; v0.16 adds no new Glyph behavior.

## Android compile gate

Before tagging `v0.16.0.beta`, GitHub Actions must pass:

```text
:app:assembleBetaDebug
:app:assemblePlayDebug
```

The tag job additionally builds `:app:assembleBetaRelease`, verifies tag/APK identity and signature, validates the release-note file, generates SHA-256 and publishes the prerelease.
