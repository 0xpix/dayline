# Dayline v0.12.6 — Validation

This patch focuses on widget typography and surface polish.

Checks performed:
- XML resource parsing
- manifest/resource reference checks
- workflow YAML parsing
- Kotlin import/reference sanity checks
- Noto Emoji downloadable-font declaration and preload checks
- verification that no legacy `emoji_*.png` assets remain
- verification that the Pulse `DAYLINE` caption is removed
- verification that widget surfaces use the full neutral system background
- update/full ZIP integrity checks

The Google Noto Emoji font file is **not bundled** with Dayline. Android requests it
from the Google Play Services downloadable-font provider and caches it on the device.

GitHub Actions remains the authoritative full Android/Gradle compile gate.

## v0.12.2 GitHub Actions repair

GitHub Actions reported one Kotlin compilation error:

`DayTimeline.kt:412` attempted to call `.dp` on a `Long`.

The calculation now ends in `.toInt().dp`.

The Node 20 / punycode lines in the uploaded Actions output are post-action
deprecation warnings and are not the cause of the Android build failure.

## v0.12.3 interaction checks

The drag bug was caused by a stale derived `dragStep` / `resizeStep` captured by
the long-running `pointerInput` coroutine. The visual offset recomposed, but the
gesture-end callback could still commit step 0. Move, resize and unscheduled-task
dragging now maintain gesture-local offset/step values and commit those directly.

Undo snackbar calls now explicitly use `SnackbarDuration.Short`.

Widget configuration contains an in-app preview using the same Noto emoji bitmap
renderer and dot-matrix renderer as the real widget.

The Now notification now displays:
- a `START → END` subtext range;
- `END HH:mm` at the beginning of the main status line;
- the existing progress bar.

GitHub Actions remains the authoritative Android/Gradle compile gate.

## v0.12.4 checks

- In-app Settings contains no widget section.
- Per-widget configuration uses modal selection sheets.
- Emoji selection uses AndroidX EmojiPickerView 1.6.0, which provides the
  complete Emoji 16.0 picker and variants.
- Widget emoji preference accepts raw Unicode glyph strings, while old enum-name
  preferences remain migration-compatible.
- Widget surfaces use `@android:color/system_neutral1_*` on API 31+ and neutral
  light/dark fallback resources before API 31.
- Calendar Provider mappings are queried during overlay refresh; provider rows
  confirmed missing/deleted are removed from local Dayline state.
- Quick Add uses FlowRow layout and common non-personal default templates.
- Upcoming includes semantic, per-calendar and per-Space filters.

GitHub Actions remains the authoritative Android/Gradle compile gate.

## Final v0.12.4 packaging validation

- Release validator: PASS
- Kotlin files: 41
- XML resources: parsed successfully
- High-risk changed Kotlin files: balanced braces/parentheses
- In-app widget settings: removed
- AndroidX complete emoji picker dependency + listener: present
- Raw Unicode widget emoji migration: present
- System-neutral widget surface: present
- External Calendar Provider deletion reconciliation: present
- Quick Add FlowRow layout + common templates: present
- Upcoming semantic/calendar/Space filters: present
- Expanded app fonts: present
- Full/update ZIP integrity: checked during packaging

The authoritative Android compile still runs in GitHub Actions because this
runtime does not include an Android SDK/Gradle build environment.

## v0.12.5 targeted validation

- Dot renderer includes `&` and exposes `canRender()`.
- Widget event pills fall back to native Unicode Glance text whenever a title
  contains emoji or another character the dot matrix cannot represent.
- Compact title truncation uses Unicode code points instead of UTF-16 indices.
- Event pills use a separate `widget_event_surface` resource in light/dark and
  dynamic-system resource sets.
- Pulse left identity width and right content padding were rebalanced around the divider.
- `SUNDAYS` and `EXCEPT_SUNDAY` recurrence values are implemented in occurrence
  logic, Android Calendar RRULEs, ICS import/export, Quick Add and descriptions.
- Quick Add uses a padded title surface and stronger 30dp section spacing.

GitHub Actions remains the authoritative Android/Gradle compile gate.

## v0.12.6 recurrence + build-log repair

The GitHub Actions log supplied after v0.12.5 exposed two compile failures:
- `DaylineTransfer.kt` had a non-exhaustive recurrence `when`.
- `DotMatrixRenderer.kt` had a malformed apostrophe character literal.

Both are repaired here.

Repeat UI now exposes only Once, Daily, Weekdays, Weekend and Choose days.
Choose days opens a seven-day checkbox dialog and stores ISO weekday values 1..7.

GitHub Actions remains the authoritative full Android compile gate.
