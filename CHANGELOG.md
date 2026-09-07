# Changelog

## 0.17.0.beta — Flow

### Added
- Added visible **FREE** gaps directly to Today with quick actions for Event, Task and Focus.
- Added a dedicated **ALL DAY** strip above the timed timeline.
- Added task scheduling windows with optional earliest/deadline dates and deadline-aware Fit into my day suggestions.
- Added Quick Move v2 choices for before/after a block, tomorrow morning/afternoon and next free slot.
- Added actionable conflict resolution with overlap duration and real free-slot suggestions.
- Added Search commands for this week, next week, weekdays and local free-time queries such as `free Friday afternoon`.
- Added richer beta diagnostics: last widget refresh, next reminder, active Focus state, Glyph last-frame time, disconnect/recovery/send-failure counters and a shareable text debug report.
- Added recurrence/planning unit tests to CI.

### Changed
- Today move/resize now snaps every 5 minutes, gives stronger 15-minute haptics, uses a larger resize target, exposes clearer floating time previews and can auto-scroll during long drags.
- Past events fade gently and Today opens near the useful part of the current day rather than always at the top.
- Month selection, weekends and busy-day overflow are more legible; Day Preview is split into ALL DAY / AGENDA / FREE TIME with an OPEN DAY action.
- Task fitting respects earliest/deadline bounds.
- Android Calendar reads, reconciles and writes timed events in their provider timezone; Quick Add preserves timezone and v0.17 task scheduling metadata when editing an existing item.
- Widgets refresh after reboot, app replacement, manual clock changes, timezone changes and date changes instead of waiting for the next app mutation.
- Updater network parsing now preserves Added / Changed / Fixed headings, shows concise publish-date/APK-size metadata and verifies the reported APK byte size before install when GitHub provides it.
- Glyph transport recovery behavior is unchanged; v0.17 only adds local instrumentation around successful frames and failures.

### Fixed
- Fixed the real updater parser stripping the section headings before the separate changelog cards could render them.
- Fixed all-day events consuming timed free-space calculations or creating false overlap conflicts.
- Hardened monthly recurrence semantics so 29th/30th/31st series do not silently shift into shorter months.
- Fixed timed Android Calendar events losing their provider timezone after a Dayline edit.
- Fixed widgets remaining visually stale after system time/date/timezone changes.
- Bumped beta versionCode to **1700** and beta versionName to `0.17.0.beta`.

## 0.16.1.beta — Navigation + updater polish

### Added
- Added visually separated updater cards for **Added**, **Changed** and **Fixed** release-note sections.

### Changed
- Removed the global page slide/fade transition so main destinations switch instantly and cleanly.
- Kept Today → Upcoming left-swipe and Upcoming → Today right-swipe gestures without animating the whole page.

### Fixed
- Fixed updater notes appearing like one uninterrupted changelog block despite having section headings.
- Bumped beta versionCode to **1601** and beta versionName to `0.16.1.beta`.

## 0.16.0.beta — Planning

### Added
- Added a planning-first Month view with compact multi-item day indicators and a tap-to-open Day preview.
- Added local free-time detection that merges busy blocks, respects buffers and exposes useful gaps without network/AI processing.
- Added persistent task duration estimates plus **Fit into my day** suggestions for matching free blocks over the next seven days.
- Added a compact event detail sheet with **Quick Move** actions for Later today, Tomorrow and Next free slot.
- Added hidden beta diagnostics behind five taps on **Settings → About → Build**.
- Added swipe navigation: swipe left on Today to open Upcoming; swipe right on Upcoming to return to Today.

### Changed
- Added a directional horizontal slide/fade animation between Today and Upcoming.
- Moved Upcoming's crowded permanent filter chip rows into one compact Filter sheet while preserving Today/Tomorrow/7 days/All and event/task/focus/meeting/holiday/calendar/Space filtering.
- Month selection now opens a day preview with its agenda and free-time windows instead of permanently expanding an agenda under the grid.
- Scheduled tasks now reserve their estimated duration in free-time and overlap calculations.

### Fixed
- Quick Move on a recurring event now detaches only the selected occurrence rather than moving the master series.
- Kept the v0.15 Daily Flow, updater, notification, Calendar sync and Glyph reliability work intact.
- Bumped beta versionCode to **1600** and beta versionName to `0.16.0.beta`.

## 0.15.4.beta — Update sheet + Focus timer polish

### Added
- Added a clean in-app **What's new** layout that parses updater-facing release notes into readable **Added / Changed / Fixed** groups.

### Changed
- Replaced the text-only update action with a distinct full-width **Download & update** button.
- After a verified APK download, Dayline now immediately opens Android's installer; when install permission is required, the action becomes **Continue update**.
- Narrowed the stacked Focus timer digits from 5 columns to 4 columns and centered each two-digit line with two columns of side padding.

### Fixed
- Fixed the update sheet rendering raw release Markdown and mixing changelog content with download/install actions.
- Fixed Focus timer digits appearing too close to or over the visible Phone (4a) Pro Glyph Matrix border.
- Bumped beta versionCode to **1504** and beta versionName to `0.15.4.beta`.

## 0.15.3.beta — Glyph stability hotfix

### Added
- Added a conservative Glyph recovery watchdog that waits for Nothing's Matrix service to recover naturally before rebuilding the SDK binding.

### Changed
- Serialized Glyph SDK callback state changes onto the main looper to avoid binder-thread races with the animation renderer.
- Replaced rapid 750 ms reconnect churn with a delayed recovery path that performs a clean `unInit → init` only after a sustained disconnect and backs off repeated attempts.
- Suppressed duplicate unchanged frames inside the hardware bridge so the old 4-second recovery heartbeat no longer reaches the Matrix hardware.

### Fixed
- Fixed the regression where the v0.14.9 reconnect/heartbeat strategy could make eye animations and Focus timer frames freeze more often and for longer.
- Fixed repeated SDK reinitialization while Nothing's proxy service was already trying to reconnect.
- Bumped beta versionCode to **1503** and beta versionName to `0.15.3.beta`.

## 0.15.2.beta — Daily Flow release alignment

### Added
- Added updater-facing `docs/releases/v0.15.2.beta.md` with concise **Added / Changed / Fixed** notes.

### Changed
- Republished the v0.15 Daily Flow milestone under a clean installable beta version after the `0.15.0`/`0.15.1` release-tag attempts could not publish a matching APK.
- Kept the Daily Flow feature set unchanged: Today timeline interactions, two-way Calendar sync, recurrence polish, Upcoming filters, reminder reliability, navigation polish and the two-line Focus Glyph timer.

### Fixed
- Fixed tagged release identity by bumping the embedded beta APK to **versionCode 1502** and **versionName `0.15.2.beta`**, matching tag `v0.15.2.beta`.

## 0.15.0.beta — Daily Flow

### Added
- Added **two-way Android Calendar reconciliation** for Dayline-mapped events: provider title, date/time, recurrence, exclusions and calendar moves now flow back into Dayline, while provider deletion removes the mapped local row.
- Added Upcoming **Today / Tomorrow / 7 days / All** windows plus a dedicated **Focus** filter.
- Added a required per-beta release-note file so every future in-app update shows concise **Added / Changed / Fixed** notes instead of autogenerated GitHub noise.

### Changed
- Reworked Today drag/resize interaction so move gestures live on the event body and the bottom resize handle is an independent gesture target.
- Today now previews the changing end time and duration while resizing, uses a clearer resize handle, and has calmer spacing/current-time styling.
- Recurring edits now default to **This occurrence** and use clearer **This + following** / **Entire series** choices; series splitting keeps past/future exclusions on the correct segment.
- Upcoming rows now open the exact occurrence while task completion remains a separate control.
- Navigation selection uses fixed geometry so selecting a destination does not shift the row.
- Focus Glyph time announcements now use a large **two-line timer**: minutes on top, seconds underneath.

### Fixed
- Fixed the Today bug where holding/dragging the bottom of an event could move its start time instead of changing the end time.
- Fixed reminder cleanup and exact-alarm fallback so revoked/changed exact-alarm permission does not break the reminder resync path.
- Added reminder/Now Activity rescheduling after reboot, app replacement, manual clock changes, timezone changes and date changes.
- Kept the v0.14.9 Glyph reconnect/watchdog/heartbeat reliability fixes and applied them unchanged to this milestone.
- Bumped beta versionCode to **1500** and beta versionName to `0.15.0.beta`.

## 0.14.9.beta — Glyph freeze recovery

- Fixed a stale Nothing Glyph Matrix service connection that could leave the hardware frozen on a timer or eye frame.
- Added automatic reconnect after service disconnect or frame-send failure.
- Added render-loop exception recovery so one bad tick cannot permanently stop eye animation.
- Added a lightweight stable-frame heartbeat so the current face is refreshed if the hardware silently drops a frame.
- Bumped beta versionCode to **1409** and beta versionName to `0.14.9.beta`.

## 0.14.8.beta — Settings cleanup

- Removed the **Widgets** section from app Settings so widget font, emoji and behavior stay only in each widget's own configuration screen.
- Increased the main Settings title and section-heading hierarchy so section titles are easier to distinguish from individual options.
- Simplified the main Glyph section to a single Dayline Glyph entry instead of showing hardware and explanatory copy inline.
- Rebuilt the Glyph settings sheet into compact **Glyph**, **Look**, **Focus**, **Night** and **Test expressions** sections.
- Reduced the Glyph preview footprint and removed long explanatory text that made the sheet feel crowded.
- Simplified Glyph option labels and kept only concise status/help text where it is useful.
- Kept the v0.14.7 Focus checkpoint behavior unchanged: 30-second time announcements at phase start, every five-minute remaining checkpoint and 1:00 remaining.
- Cleaned Beta Updates and About by removing redundant copy and combining build code + commit information.
- Bumped beta versionCode to **1408** and beta versionName to `0.14.8.beta`.

## 0.14.7.beta — Focus time announcements

- Removed the persistent Focus timer layout from the Glyph Matrix; Focus and Rest now keep the normal large expressive eyes most of the time.
- Added temporary **30-second `MM:SS` announcements** that replace the eyes completely instead of mixing timing UI with the face.
- Added clean timing transitions: `eyes → CENTER → time → CENTER → eyes`.
- Implemented Option B checkpoint timing for every Focus/Rest phase: announce the **phase start**, then every **5-minute remaining checkpoint**, then **1:00 remaining**.
- 25/5 Focus now announces 25:00, 20:00, 15:00, 10:00, 05:00 and 01:00; Rest announces 05:00 and 01:00.
- 50/10 and custom Focus cycles derive the same checkpoints automatically.
- The 30-second announcement is a live countdown, so `20:00` continues toward `19:31` before the eyes return.
- Paused sessions keep the remaining time frozen while an announcement is visible.
- Normal expression animation remains unchanged outside announcement windows, including the existing `CENTER → expression → CENTER` contract.
- Bumped beta versionCode to **1407** and beta versionName to `0.14.7.beta`.

## 0.14.6.beta — Focus timer Glyph

- Replaced the Focus progress ring with a dedicated **top eyes + bottom `MM:SS` timer** layout on the 13×13 Glyph Matrix.
- Kept normal idle mode's large expressive eyes unchanged.
- Added a separate compact Focus eye renderer so animations stay in the top rows while the timer owns rows 8–12.
- Left the middle rows intentionally empty so Focus timing never collides visually with the face.
- Reused the same `CENTER → expression → CENTER → next expression` animation state machine during Focus and Rest.
- Made Focus Look Left/Look Right shift the entire compact eye pair left/right, matching the behavior of normal idle eyes.
- Kept Blink, Wink, Happy, Hearts, Squint and rare Sleepy available above the timer.
- Added a compact 3×5 numeric font that fits `MM:SS` exactly across all 13 matrix columns.
- Focus, Rest and paused sessions now show their actual remaining phase time directly; 25/5, 50/10 and custom cycles are supported.
- Kept duplicate-frame suppression so the once-per-second countdown does not cause unnecessary extra frame traffic.
- Bumped beta versionCode to **1406** and beta versionName to `0.14.6.beta`.

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
