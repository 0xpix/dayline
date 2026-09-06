# Changelog

## 0.12.7 — Emoji picker and visual polish

- Added a scrollable monochrome Noto Emoji widget picker.
- Removed yellow system emoji rendering from widget configuration.
- Made active notification progress visible immediately and refresh every minute.
- Unified all Material typography roles under the selected app font.
- Changed the floating Today/Home control to the Dayline app-logo geometry.

## 0.12.6 — Repeat-day chooser

- Replaced Sunday-specific/weekly/monthly visible choices with Once, Daily,
  Weekdays, Weekend and Choose days.
- Added seven-day checkbox picker for custom repeat schedules.
- Added repeat-day persistence, Android Calendar RRULE support and ICS support.
- Fixed the v0.12.5 DotMatrixRenderer apostrophe syntax error.
- Fixed the v0.12.5 non-exhaustive DaylineTransfer recurrence branch.
- Improved Quick Add section hierarchy.

## 0.12.5 — Unicode widget and recurrence polish

- Preserved emoji, ampersands and other Unicode in widget event names.
- Added a visible system-neutral event-name surface to widgets.
- Rebalanced Pulse padding around emoji, divider and information.
- Added Sunday-only and every-day-except-Sunday recurrence modes.
- Updated Android Calendar and ICS recurrence mappings.
- Improved Quick Add title/section spacing.

## 0.12.4 — Settings, sync and Upcoming polish

- Moved all widget settings into the per-widget configuration activity.
- Replaced cycling widget selectors with popup/sheet selectors.
- Added the full AndroidX Emoji 16.0 picker for widget emoji selection.
- Made long-title sliding a per-widget option.
- Switched widget light/dark surfaces to Android system-neutral dynamic colors.
- Reconciled externally deleted synced-calendar events.
- Cleaned and expanded app font choices.
- Reworked Quick Add spacing and default templates.
- Added semantic/calendar/Space filters to Upcoming.

## 0.12.3 — Interaction and layout polish

- Fixed drag-to-move and drag-to-resize returning to the previous time.
- Fixed drag scheduling for unscheduled tasks.
- Undo snackbar now auto-dismisses.
- Added lower Dawn-inspired vertical staging to Today, Tasks, Spaces, Settings and Task detail.
- Refined floating controls and navigation sheet.
- Added a live widget configuration preview.
- Reduced awkward Pulse right-side whitespace.
- Made event end time explicit in Now notifications.

## 0.12.2 — Build repair

- Fixed `DayTimeline.kt` compilation: a `Long` value was incorrectly used with
  Compose's `.dp` extension.
- Preserved all v0.12.1 Noto Emoji and widget-surface changes.

## 0.12.1 — Noto widget polish

- Replaced custom widget emoji PNG assets with Google Fonts Noto Emoji glyph rendering.
- Added downloadable Noto Emoji font preloading through Google Play Services.
- Removed the `DAYLINE` caption under the Pulse widget emoji.
- Made widget bodies use a full neutral system surface for reliable light/dark contrast.
- Removed transparent widget configuration to avoid wallpaper-dependent text contrast.

## 0.12.0 — Play beta

- Added per-calendar controls and Space-to-calendar routing.
- Added recurring edit scopes and provider exclusions for single-occurrence edits.
- Added timeline move/resize/tap-to-create/task scheduling interactions.
- Expanded focus cycles to 25/5, 50/10 and custom with ongoing controls/statistics.
- Added event progress, Today summary, overlap warnings and buffers.
- Added templates and richer task scheduling/priority/subtask flows.
- Added undo, search, JSON backup/restore and ICS import/export.
- Added per-widget configuration and current/focus/rest widget states.
- Added onboarding and refined haptics.
- Added MIT license, contribution/security/privacy documentation and Play-ready signed AAB workflow.

## 0.11.1

- Compile repair for Today ribbon and current-time line.
