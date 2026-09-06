# Contributing to Dayline

Dayline is an open-source Android calendar/planner. Small, focused pull requests are preferred.

## Development

Requirements:
- JDK 17
- Gradle 9.4.1
- Android SDK API 37 / Build Tools 36.0.0

Build a debug APK:

```bash
gradle :app:assembleDebug --no-daemon
```

Before opening a pull request:
1. Keep the UI minimal and accessible in both light and dark mode.
2. Do not add analytics, advertising, tracking, or network dependencies without discussion.
3. Do not commit signing keys, credentials, generated APKs/AABs, or `local.properties`.
4. Test calendar permission denial as well as granted access.
5. Mention any data-format or calendar-provider migration in the pull request.

## Design principles

- Today first.
- Every decorative mark should communicate something.
- Prefer Android platform behavior over custom infrastructure.
- Keep personal calendar/task data on-device unless a future sync feature is explicitly opt-in.
