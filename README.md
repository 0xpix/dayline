# Dayline — v0.2.0

A calm, typography-first Android calendar for Nothing OS / Android, built with Kotlin, Jetpack Compose and Material 3 underneath a custom visual language.

## What changed in v0.2.0

Dayline is no longer a static prototype.

- Real local event and task creation.
- Items persist after closing the app.
- Edit and delete existing items.
- Complete tasks per occurrence.
- Real navigation: Today, Calendar, Upcoming, Tasks, Settings.
- Minimal month calendar with day selection and event dots.
- Upcoming view for the next 30 days.
- Functional settings for System / Light / OLED dark appearance, orb visibility and week start.
- Recurrence choices directly in Quick Add:
  - Once
  - Daily
  - Weekdays
  - Weekly
  - Monthly

Weekly recurrence follows the weekday of the selected start date. Monthly recurrence follows the selected day of the month. Recurring task completion is stored per date, so completing today's recurring task does not remove the next occurrence.

## Quick Add design

The add sheet stays intentionally small and explicit:

1. Title
2. Event or Task
3. Date and optional time
4. Repeat frequency
5. Add

There is no large form. Recurrence is visible as a row of simple choices, with a plain-language summary underneath such as `Every Saturday.` or `Only once.`

## Storage

v0.2.0 is a standalone local calendar. Data is stored privately on the device using Android SharedPreferences with JSON serialization. It does not yet read or write Google Calendar / Android Calendar Provider data.

## Toolchain

- Kotlin 2.3.21
- Android Gradle Plugin 9.2.0 with built-in Kotlin
- Compose BOM 2026.08.00
- compileSdk 37
- targetSdk 36
- minSdk 26
- JDK 17

## Build an APK

Push to `main` to build a test APK in GitHub Actions.

Create and push a version tag to publish an APK under GitHub Releases:

```bash
git tag v0.2.0
git push origin v0.2.0
```

The release workflow will attach `dayline-v0.2.0-debug.apk`.

## Next

v0.3.0 is planned for Android Calendar Provider integration so Dayline can display and create device / Google calendar events while preserving the same minimal UI.
