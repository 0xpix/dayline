# Dayline v0.17.1.beta — Validation Report

This report tracks the current **v0.17.1.beta / versionCode 1701** source. GitHub Actions remains the authoritative Android/Compose compile gate.

## Release checks

- Beta target: **0.17.1.beta / 1701**.
- `docs/releases/v0.17.1.beta.md` contains concise **Added / Changed / Fixed** updater notes.
- Beta/Play flavor separation remains intact; Play stays on the stable base version and does not package the Nothing SDK.
- Tagged builds must verify signed APK versionName/versionCode against tag `v0.17.1.beta`.
- Static validation, recurrence/planning unit tests, Beta debug compile and Play debug compile must all pass before tagging.

## Today Flow

- Today exposes meaningful free gaps directly in the timeline.
- Tapping a free gap offers Event / Task / Focus actions.
- All-day events render in a separate strip and do not consume timed free-space calculations.
- Move and resize remain independent gesture targets.
- Move/resize snap every five minutes, with stronger 15-minute haptic landmarks.
- Resize uses a larger invisible touch target while keeping the visual handle minimal.
- Drag/resize previews show exact start/end information and can auto-scroll the Today column.
- Past timed items fade without being removed from the day.
- Today initially positions near the current time; a Month-selected date positions near its first useful block.
- The Today control still returns a historical Month-opened day to the real current day, and on the real current day returns the timeline to now.

## Month + Day Preview

- Today, selected date and weekends have distinct but restrained treatments.
- Up to three event/task dots remain compact; extra density receives a small overflow mark.
- Day Preview separates ALL DAY / AGENDA / FREE TIME.
- OPEN DAY navigates to that exact date in the Today timeline.
- Tasks can still be completed from the preview.

## Planning + tasks

- `estimatedDurationMinutes` remains persisted and defaults to 30 minutes.
- Optional `earliestDate` and `deadlineDate` are persisted for tasks.
- Fit into my day uses the task scheduling window and refuses suggestions after the deadline.
- Busy intervals merge buffers and ignore all-day entries.
- Search free-time commands remain deterministic and local-only.

## Quick Move + conflicts

- Quick Move offers Before this block, After this block, Tomorrow morning, Tomorrow afternoon and Next free slot when valid.
- One-off moves retain Undo.
- Recurring moves still detach only the selected occurrence through `SeriesEditor.THIS_OCCURRENCE`.
- Conflict UI shows overlap duration and offers a real post-conflict / next-free position.
- Keeping an overlap remains an explicit option.

## Search

- Existing text, Space, calendar, Today/Tomorrow, task/event/focus and month searches remain supported.
- This week / next week and weekday tokens resolve locally.
- `free Friday afternoon`-style queries return tappable free windows that open event creation at that time.
- Normal results are grouped by occurrence date.

## Calendar + timezone behavior

- Provider all-day instances are interpreted in UTC and remain separate from the timed day rail.
- Timed provider instances are interpreted with `EVENT_TIMEZONE` when available instead of silently forcing the device timezone.
- Mapped-event reconciliation stores all-day state and timezone identity alongside title/date/time/recurrence changes.
- Timed writes set `EVENT_TIMEZONE` / `EVENT_END_TIMEZONE` and calculate DTSTART/DTEND/EXDATE using the event timezone.
- Recurrence UNTIL conversion preserves the intended event-zone end date while serializing the provider rule in UTC.
- Calendar deletion/reconciliation behavior from v0.15 remains intact.

## JVM regression tests

CI runs pure JVM tests before Android assembly.

- Monthly series on the 31st skip shorter months instead of sliding to month-end.
- Excluded dates and recurrence end dates are enforced.
- All-day entries do not consume free time.
- Buffered busy windows merge correctly.
- Task suggestions obey earliest/deadline bounds.
- THIS OCCURRENCE detaches only the selected recurring occurrence.
- THIS + FOLLOWING keeps past/future exclusions on the correct series segment.
- ENTIRE SERIES preserves mapped Calendar provider identity.
- Beta version comparison/versionCode installability rules are covered.
- Glyph Center geometry stays symmetric and solid, Look left/right shift the full face by one column, removed legacy expressions fall back to Center, and Focus timer pixels stay inside the intended Matrix columns.

## Widgets

- Normal app mutations still trigger a Glance refresh and record the last refresh time.
- Reboot, app replacement, manual clock changes, timezone changes and date changes now also resync widgets in addition to reminders/Now Activity.
- Widget configuration remains owned by the widget configuration screen, not main app Settings.

## Updater

- Candidate updates must be newer by both semantic beta version and Dayline's versionCode convention.
- Cached update cards use the same installability rule, so stale releases are not resurfaced after an app upgrade.
- The update sheet shows live percentage progress while the APK downloads and switches to a verification state after transfer completes.
- Downloaded APK package, versionCode, byte size and SHA-256 checksum are verified before install.
- The downloaded APK signing certificate is compared with the currently installed Dayline beta before Android's installer opens.
- GitHub release-body cleanup preserves literal `## Added`, `## Changed`, `## Fixed` headings.
- Settings continues rendering each heading in a separate card.
- APK size is retained in `BetaRelease`, shown as concise release metadata and verified against the downloaded file when GitHub reports a size.
- Package/version/checksum verification remains mandatory before the installer opens.
- GitHub 404/rate/network failures use concise current errors rather than claiming a public repository is private.

## Backup / restore

- Backup schema is now explicit at version 2.
- Restore rejects unknown future schema versions before clearing existing state.
- Nested event/task JSON and Space records are decoded and validated before any current data is cleared.
- Selecting a backup shows event, task and Space counts and requires an explicit RESTORE action before replacement.
- Transient updater state, sync-health timestamps and widget-instance cache entries are excluded from backup payloads.
- Legacy version-1 backups remain accepted.

## Release discipline

- `scripts/bump_beta.py` prepares versionName/versionCode and release-facing metadata together.
- `scripts/check_beta_bump.py` compares functional changes against the latest beta tag.
- Android CI fetches full tag history and fails when functional app/build changes exist without a newer beta version.
- Static validation derives the active beta version from Gradle and verifies README, CHANGELOG and updater release-note alignment.

## Beta diagnostics

- Available only in the GitHub beta channel behind five taps on Settings → About → Build.
- Reports build/channel, Calendar sync, last widget refresh, next scheduled reminder, active Focus state and updater status.
- Glyph diagnostics report last successful frame timestamp, disconnect count, recovery count, send-failure count and last error text when present.
- Export Debug Log shares only diagnostic facts; event/task titles and calendar contents are excluded.

## Glyph reliability baseline

- v0.17 adds no new eye expression, Focus checkpoint or recovery behavior.
- Conservative v0.15.3 recovery remains unchanged.
- Instrumentation records transport outcomes without sending additional frames or creating a new heartbeat.
- Focus timer remains the centered stacked minutes/seconds layout.

## Navigation + accessibility

- Main destination changes remain instant; the disliked full-page animation stays removed.
- Today ↔ Upcoming swipe shortcuts remain available.
- Settings selector rows, compact text actions, calendar visibility controls and Glyph expression tests use larger touch targets while keeping visual chrome small.
- The visual Glyph Matrix preview exposes one concise accessibility description instead of exposing 169 individual pixels.
- Typography continues using Material hierarchy so larger Android font scales can expand without fixed-height text clipping where practical.

## Android compile gate

Before tagging `v0.17.1.beta`, GitHub Actions must pass:

```text
:app:testBetaDebugUnitTest
:app:testPlayDebugUnitTest
:app:assembleBetaDebug
:app:assemblePlayDebug
```

The tag job additionally builds `:app:assembleBetaRelease`, verifies tag/APK identity and signature, validates the release-note file, generates SHA-256 and publishes the prerelease.
