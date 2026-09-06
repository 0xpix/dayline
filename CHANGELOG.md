# Changelog

## 0.13.0.beta — Update & reliability beta

- Fixed the beta/play debug compile gate after CI exposed two integration regressions: the scheduled beta checker now calls its suspend API from an IO coroutine, and Settings again receives all widget preference values/callbacks.
- Rebuilt the GitHub beta updater around the public Releases list so prereleases are detected correctly.
- Added update states, release notes sheet, verified APK download/install, last-checked state and friendly network errors.
- Added optional once-daily beta checks with a low-impact AlarmManager schedule.
- Added build/channel/commit identity in Settings while preserving the beta/play flavor split.
- Added a keystore-alias preflight and APK signature verification to the tagged beta workflow.
- Added calendar sync health and per-calendar SYNCED / HIDDEN / READ ONLY state.
- Refined Now notifications with minute-level text refresh and compact focus-session dots while keeping the system chronometer removed.
- Expanded undo to Task → Event conversion in addition to move, resize, schedule and delete.
- Made conflict markers tappable and added a compact overlap explanation sheet.
- Expanded Search with Today/Tomorrow/unfinished/focus/task/event/month commands and exact-occurrence navigation.
- Polished Today with a visible date marker, larger resize handle, exact drag/resize previews, haptic snaps and return-to-now behavior.

## 0.12.8.beta — Beta version alignment

- Aligned the app, README, validator, docs and issue template on `v0.12.8.beta`.
- Beta flavor reports `0.12.8.beta`; Play flavor reports `0.12.8`.
- Bumped Android `versionCode` to 33 so the beta updater recognizes newer builds.
- GitHub Actions accepts the `v*.beta*` tag convention while retaining compatibility with older `v*-beta*` tags.
- Beta updater reads GitHub Releases, downloads the beta APK, verifies SHA-256/package/version and hands installation to Android.
- Stable Play flavor has no INTERNET or REQUEST_INSTALL_PACKAGES permission.
- GitHub Actions signs beta releases with a dedicated persistent keystore and publishes APK + checksum as a prerelease.
- Play workflow explicitly builds the `playRelease` APK/AAB.

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
