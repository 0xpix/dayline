# Changelog

## 0.14.5.beta — Updater + Glyph preview fix

- Fixed beta update discovery so a GitHub release must be newer by both semantic version and Android `versionCode` before Dayline offers it.
- Added an early same-code/older-build guard before downloading an APK, with a clearer error message.
- Added a tagged-release workflow verification step that checks the signed APK's embedded `versionName` and `versionCode` against the Git tag before publishing.
- Cleaned generated GitHub release notes so raw changelog URLs are not dumped into the in-app release sheet.
- Rebuilt the Glyph Settings preview as a single circular surface; unlit cells no longer form a visible square inside the circle.
- Made every Settings preview expression animate as `CENTER → expression → CENTER`, matching the live Glyph contract.
- Increased live Center recovery from ~500 ms to ~700 ms so transitions are visibly separated.
- Bumped beta versionCode to **1405** and beta versionName to `0.14.5.beta`.

## 0.14.4.beta — Centered Glyph transitions

- Made **every non-center Glyph animation** transition through Center before another animation can begin: `CENTER → animation → CENTER → next animation`.
- Applied the same Center recovery rule to Blink so expressions and blinks never visually blend together.
- Added a dedicated ~500 ms Center recovery beat after every expression, not only left/right glances.
- Delayed any due blink or motion until after the Center recovery window so two animations cannot start on the same renderer tick.
- Bumped beta versionCode to **1404** and beta versionName to `0.14.4.beta`.

## 0.14.3.beta — Glyph polish

- Removed Curious, Playful, Surprised, Side-eye, Excited and Rolling from the live eye loop and Settings preview.
- Rebalanced idle behavior around Center, left/right glances, Happy, Wink, Hearts, Squint and rare Sleepy expressions.
- Added a dedicated Center recovery after left/right glances.
- Reworked Hearts into smaller, cleaner heart-eye patterns.
- Rebuilt Focus progress as a circular perimeter around the face with a faint full outline and brighter completed pixels.
- Focus advances clockwise; break advances in reverse while the eyes keep animating.
- Mapped Dayline's 0–100% brightness control to the higher raw `IntArray` intensity range demonstrated by Nothing's official Glyph Matrix example project.
- Kept the Settings brightness UI on a normal percentage scale while allowing raw Matrix output up to 2047 internally.
- Kept duplicate-frame suppression to reduce visible Glyph twitching/flicker.
- Bumped beta versionCode to **1403** and beta versionName to `0.14.3.beta`.

## 0.14.2.beta — Eyes + Focus redesign

- Removed automatic calendar/app-state takeovers from the live Glyph experience.
- Kept expressive eyes active continuously and made Focus Mode the only automatic Dayline overlay.
- Added Focus progress for 25/5, 50/10 and custom focus cycles while retaining normal eye animation.
- Increased blink frequency and substantially increased Happy frequency while making Sleepy rare.
- Added persistent Glyph brightness control to Settings.
- Simplified Glyph settings around enable/hardware, look & feel, Focus, night behavior and expression previews.
- Added frame-change suppression so identical frames are not resent unnecessarily.
- Bumped beta versionCode to **1402** and beta versionName to `0.14.2.beta`.

## 0.14.1.beta — Glyph readability pass

- Enlarged the Phone (4a) Pro eyes to a bold 5×5 rounded dot-eye shape.
- Corrected left/right eye movement with the larger eyes.
- Reworked Blink timing so the closed frame remains visible on hardware.
- Simplified weak/crowded app-state symbols into larger centered 13×13 patterns.
- Improved Sleepy and the main expression patterns for better small-matrix readability.
- Bumped beta versionCode to **1401** and beta versionName to `0.14.1.beta`.

## 0.14.0.beta — Dayline Glyph beta

- Added a Phone (4a) Pro 13×13 Glyph Matrix integration for GitHub beta builds.
- Added Off / Eyes only / Eyes + app states modes and persistent Glyph preferences.
- Added solid eye expressions with corrected left/right direction and fast center → blink → center animation.
- Added natural blink/glance behavior with Rare / Normal / Frequent glance cadence.
- Added brief app-state signals for upcoming events, reminders, focus/rest, task completion, conflicts, free blocks/day open, event start/end, moved items, sync status and go/leave cues.
- Added priority/expiry queueing so important Dayline states interrupt the eyes without taking over permanently.
- Added subtle/active focus behavior, optional rest animation, quiet hours, night dimming and reduced motion.
- Added a live in-app 13×13 preview plus hardware test controls.
- Added a beta-only AOD Glyph Toy service and shortcut to Nothing's Glyph Toys manager.
- Kept the proprietary Nothing SDK out of the repository; GitHub beta CI downloads it from Nothing's official developer kit at build time.
- Kept the Play flavor free of the Glyph Matrix SDK pending commercial licensing.
- Added compatibility fallback for SDK builds that document Phone (4a) Pro but do not yet expose `Glyph.DEVICE_25111p`.
- Bumped Android versionCode to 1400 and beta versionName to `0.14.0.beta`.

## 0.13.1.beta — Release workflow hotfix

- Fixed GitHub beta-release signature verification: the workflow now calls the `apksigner` binary from Android Build Tools 36.0.0 directly instead of assuming it is on the shell `PATH`.
- Added the same explicit APK signature verification to the Play release workflow.
- Bumped Android versionCode to 1301 so this hotfix can be installed over 0.13.0/0.12.8 beta builds.

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
