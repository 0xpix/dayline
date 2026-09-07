# Dayline v0.15.4.beta — Validation Report

This report tracks the current v0.15.4.beta source state. GitHub Actions remains the authoritative Android/Compose compile gate.

## Release checks

- Beta version target: **0.15.4.beta / versionCode 1504**.
- Static project validator must pass.
- Beta/Play flavor and permission separation must remain intact.
- Tagged beta workflow verifies the signed APK's embedded `versionName` and `versionCode` against the Git tag before publication.
- Tagged beta workflow requires `docs/releases/<tag>.md` with **Added / Changed / Fixed** sections and publishes that file as the GitHub release body used by the in-app updater.
- `docs/releases/v0.15.4.beta.md` exists for this tag.
- GitHub beta updater requires a release to be newer by both semantic version and Android `versionCode`.
- Proprietary Nothing `glyph-matrix-sdk-2.0.aar` remains absent from source and is fetched only by beta CI.
- Play workflow does not fetch/package the Glyph Matrix SDK.
- Automatic Android app-data backup remains disabled.
- Notification seconds-chronometer / `HH:mm:ss` regression scan remains required.
- Merge-marker / duplicate-import scan remains required.

## Today interaction validation

The v0.15 Today timeline uses separate gesture ownership.

- Long-press + drag on the **event body** moves start and end together in 15-minute steps.
- Dragging the **bottom handle** changes only `endTime`; it cannot bubble into the move gesture.
- The changing end time is shown live as `END HH:mm` while resizing.
- The left-side end label and event rail height preview the resized duration before release.
- Minimum resized duration is 15 minutes.
- Gap/empty-timeline taps still create an event at the tapped 15-minute time.
- Current-time marker uses a quieter monochrome line and Today spacing is less crowded.
- Direct Today move/resize of a recurring item applies to the visible occurrence, not the entire master series.

## Android Calendar two-way validation

For Dayline events with `calendarEventId`, Calendar Provider reconciliation handles both directions.

- Dayline edits continue to write through `AndroidCalendarSync.upsert()`.
- Provider deletion removes the mapped local Dayline row.
- Provider changes to title, start date/time, end time/all-day state, RRULE, EXDATE and calendar placement are pulled back into the mapped Dayline row.
- `DaylineApp` compares the full reconciled list, not only list size, so same-count edits are persisted.
- Reconciled changes rebuild reminders/Now Activity and refresh widgets.
- Provider query failures remain conservative and keep local data instead of deleting it.
- External recurring generated instances remain read-only until provider-native per-occurrence exception editing is implemented.

## Recurrence validation

- **This occurrence** is the safe default when opening a recurring occurrence.
- **This occurrence** excludes the selected date from the master and creates a detached one-off carrying the edited date/time.
- **This + following** closes the old segment the day before the selected occurrence and starts a linked new segment.
- Past exclusions stay with the old segment; current/future exclusions move to the new segment.
- **Entire series** preserves the master identity/provider mapping and does not accidentally adopt the opened occurrence date unless the user explicitly changes the date.
- Recurring Today drag/resize uses **This occurrence** semantics and supports Undo.
- The delete affordance explicitly says **Delete series** for recurring masters so it does not imply a per-occurrence delete that is not implemented there.

## Upcoming validation

Upcoming supports two independent dimensions:

- **When:** Today / Tomorrow / 7 days / All.
- **Show:** All / Events / Tasks / Focus / Meetings / Holidays / calendar / Space.

Rows open the exact occurrence date. Task completion is a separate control so tapping the task itself no longer unexpectedly marks it done.

## Notification / Now Activity reliability

- Reminder alarms are rebuilt on boot and app replacement.
- Reminder and Now Activity schedules are also rebuilt after manual clock changes, timezone changes and date changes.
- Exact reminder scheduling falls back to `setAndAllowWhileIdle` if exact-alarm permission is unavailable or changes between the permission check and schedule call.
- Deleting/canceling an item clears both its alarm and any shown reminder notification.
- Now Activity retains minute-level clean text rather than Android's seconds chronometer.

## Navigation validation

Navigation selection keeps a fixed 16dp indicator slot and a fixed-size dot. Selection changes color/opacity only, so rows do not shift horizontally when selected.

## Glyph reliability + Focus readability

The conservative Glyph transport introduced in v0.15.3 remains unchanged:

- SDK callbacks are posted to the **main looper** before they mutate connection state.
- `onServiceDisconnected` keeps the existing manager/callback binding alive first, allowing Nothing/Android to reconnect the proxy service naturally.
- Dayline waits about **5 seconds** before rebuilding a binding that remains disconnected.
- A forced recovery performs a clean `unInit()` before obtaining/initializing the manager again.
- Repeated recovery attempts back off up to **30 seconds** rather than looping every 750 ms.
- Only the newest pending frame is retained while disconnected.
- Connection generations reject callbacks belonging to an old binding.
- Duplicate unchanged frames are suppressed inside `NothingGlyphBridge`.
- A real send failure clears the delivered-frame cache and enters the same delayed recovery path.
- Render ticks remain protected so one unexpected renderer exception cannot permanently terminate later animation ticks.

The Focus checkpoint schedule remains unchanged:

- phase start;
- every five-minute remaining checkpoint;
- 1:00 remaining;
- each announcement lasts 30 seconds;
- transition remains `eyes → CENTER → time → CENTER → eyes`.

v0.15.4 keeps the stacked presentation but narrows each digit from **5 columns to 4 columns**. Each two-digit line is now **9 pixels wide** and centered at x=2..10, leaving two empty columns at both sides. Minutes stay on rows 1–5 and seconds on rows 7–11, preserving leading zeros while keeping the visible timer clear of the circular Matrix edge.

## Update sheet validation

The updater sheet reads the curated GitHub release body and now parses it into structured UI instead of rendering raw Markdown.

- `## Added`, `## Changed` and `## Fixed` headings become separate in-app groups.
- Markdown heading markers, bullet markers, bold markers and inline backticks are stripped from visible updater text.
- A release without recognized sections still falls back to a simple **Changed** list.
- The primary action is a full-width **Download & update** button.
- After download/checksum/package/version verification succeeds, Dayline immediately calls the Android installer.
- If Android requires install permission, the verified APK is kept and the action becomes **Continue update**.
- GitHub release navigation remains a secondary **View on GitHub** action.

## Settings / widget ownership

The v0.14.8 Settings cleanup remains intact. App Settings has no widget configuration section; widget appearance/content controls belong only to each widget's configuration screen.

## Android compile gate

Before tagging `v0.15.4.beta`, GitHub Actions must pass:

```text
:app:assembleBetaDebug
:app:assemblePlayDebug
```

For tag `v0.15.4.beta`, the tagged job additionally builds `:app:assembleBetaRelease`, verifies tag/APK version identity and signature, validates the release-note file, generates the SHA-256 checksum and publishes the prerelease.