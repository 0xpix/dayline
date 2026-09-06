# Dayline v0.13.0.beta — Validation Report

This report records checks performed on the v0.13.0.beta source package. GitHub Actions remains the authoritative Android/Compose compile gate because this execution environment does not include a complete Android SDK/Gradle toolchain.

## Release checks

- Static project validator: **PASS**
- XML resource parsing across main/beta/play source sets: **PASS**
- Manifest component/resource checks: **PASS**
- GitHub Actions workflow YAML parsing: **PASS**
- Beta/play flavor and permission separation checks: **PASS**
- GitHub beta signing workflow contract: **PASS**
- Play signing/AAB workflow contract: **PASS**
- Main/Play manifest has no beta updater `INTERNET` or `REQUEST_INSTALL_PACKAGES` permission: **PASS**
- Beta manifest contains only the updater network/install permissions plus FileProvider: **PASS**
- Automatic Android app-data backup disabled: **PASS**
- Promoted-notification API/permission path removed: **PASS**
- Notification system chronometer / `HH:mm:ss` path removed: **PASS**
- Android-independent Kotlin model/data compile: **PASS**
- Recurrence, edit-scope, ICS, overlap, custom-focus and version-ordering logic tests: **PASS**
- Source merge-marker / duplicate-import scan: **PASS**
- ZIP integrity: **PASS (full and update archives)**

## v0.13 beta updater

`beta` builds use package `com.pix.dayline.beta` and query the public `0xpix/dayline` GitHub Releases list. The checker ignores drafts, includes prereleases, compares semantic numeric versions, chooses the newest version newer than the installed beta, and prefers a tag/beta-matching APK asset.

The update UI exposes Checking / Up to date / Available / Retry states, last-check time and release notes. Downloaded APKs are checked against the release SHA-256 when present, verified to belong to the currently installed beta package and required to carry a newer Android version code before Dayline opens Android's package installer. Installation is never silent.

Automatic checks are optional, no more than daily, and use an inexact non-wakeup alarm. Calendar, task, focus, widget and backup contents are never included in the GitHub request.

The `play` flavor provides an offline updater stub and does not receive the beta updater's network/install permissions.

## Notification presentation

The Now notification does not use Android's seconds chronometer. Examples:

- `RESEARCH` + `42M LEFT · ENDS 09:00`
- `FOCUS · Research` + `18M · ●●○○○ · 2/5`
- `REST · Research` + `4M · ●●○○○ · 2/5`

The event time range remains a quiet subtext line and normal text refreshes at minute boundaries. Focus phase and event-end alarms still occur at their actual boundaries.

## CI compile repair from 2026-09-06

The first GitHub Actions compile attempt exposed two concrete Kotlin integration errors in both beta and Play debug variants:

1. `BetaUpdateScheduler` called the suspend `BetaUpdateChecker.check(...)` API from a plain executor callback. The receiver now keeps `goAsync()` alive while the check runs in `CoroutineScope(Dispatchers.IO).launch`.
2. The `SettingsScreen` integration omitted the existing widget font, emoji and auto-slide values/callbacks. All six arguments are now wired back to `DaylineStore` and refresh placed widgets after a change.

The local static validator and named-argument consistency check pass after these repairs. GitHub Actions remains the authoritative Android compiler confirmation.

## Final Android compile gate

Run on GitHub Actions before creating the release tag:

```text
:app:assembleBetaDebug
:app:assemblePlayDebug
```

For `v0.13.0.beta`, the tagged job additionally builds and verifies `:app:assembleBetaRelease`, generates its SHA-256 checksum and publishes a GitHub prerelease. Do not consider the release Android-compile-confirmed until that workflow is green.
