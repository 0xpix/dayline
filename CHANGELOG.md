# Changelog

## 0.12.1-beta.1 — GitHub beta updater

- Split distribution into GitHub `beta` and Google Play `play` flavors.
- GitHub beta uses package `com.pix.dayline.beta`, so beta and stable can coexist.
- Added Settings → Beta updates with automatic check when Settings opens.
- Downloads the newest GitHub prerelease APK in-app.
- Verifies GitHub SHA-256 checksum, APK package name and newer version code before installation.
- Uses Android's official unknown-app/install confirmation flow; never silent-installs.
- Stable Play flavor has no INTERNET or REQUEST_INSTALL_PACKAGES permission.
- GitHub Actions now signs beta releases with a dedicated secret keystore and publishes APK + checksum as a prerelease.
- Play workflow now explicitly builds the `playRelease` APK/AAB.


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
