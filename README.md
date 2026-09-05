# Dayline — v0.1.0

Working-title Android calendar inspired by the calm, typography-first feel of Dawn,
but built as a native modern Android app.

## Current build

The first milestone implements:

- Kotlin + Jetpack Compose
- Material 3 foundation with a custom visual language
- edge-to-edge Android UI
- light and OLED-friendly dark themes
- Dawn-inspired Today composition
- large greeting/date typography
- soft gradient orb
- unified event/task agenda rows
- three floating actions
- minimalist Quick Add bottom sheet
- minimalist navigation bottom sheet
- Android 16 target with Android 17/API 37 compile support

The agenda data is intentionally mocked in v0.1.0.

## Toolchain

- Kotlin 2.3.21
- Android Gradle Plugin 9.2.0
- Compose BOM 2026.08.00
- compileSdk 37
- targetSdk 36
- minSdk 26
- JDK 17

## Run

1. Open the folder in Android Studio.
2. Make sure Android SDK API 37 is installed.
3. Use JDK 17 for Gradle.
4. Let Android Studio sync Gradle.
5. Run on your Nothing OS / Android 16 device.

## Design principles

1. The calendar opens to the day, not a dashboard.
2. Typography carries hierarchy instead of cards.
3. Whitespace is functional.
4. Color is optional and restrained.
5. Events and tasks coexist in the same day.
6. Material 3 supplies platform behavior, not the visual identity.
7. Nothing-like monochrome should remain the default direction.

## Next milestone

v0.2 will replace mock data with:

- real navigation
- month calendar
- upcoming view
- Room-backed local tasks
- Android Calendar Provider read access
- event creation/editing
- theme setting: Nothing / Dawn / Material You
- first home-screen widget

## Fonts

No font files are bundled in this repository.

The initial design uses the system sans-serif family with custom weight, size,
line-height and tracking. A later milestone can use Manrope through Android's
downloadable-font mechanism without shipping a font binary in the project.

## APK without Android Studio

This repository includes `.github/workflows/build-apk.yml`.

After pushing the project to GitHub:

1. Open the repository's **Actions** tab.
2. Run **Build Android APK**.
3. Open the completed run.
4. Download the `dayline-v0.1.0-debug-apk` artifact.
5. Extract it and install the APK on Android.

The debug APK is suitable for personal testing. A later release workflow should use
a persistent signing key for upgradeable release APKs.
