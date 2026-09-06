# Dayline v0.12.0 — Validation Report

## Release checks completed

- Static release validator: **PASS**
- XML resource parsing: **PASS**
- Manifest component/reference checks: **PASS**
- GitHub Actions workflow YAML parsing: **PASS**
- Play release workflow/signing configuration checks: **PASS**
- Privacy-policy / Data Safety consistency checks: **PASS**
- No `INTERNET` permission: **PASS**
- Android automatic backup disabled: **PASS**
- Promoted-notification permission/API removed: **PASS**
- Notification chronometer / `HH:mm:ss` path removed: **PASS**
- Core Kotlin model/data compile: **PASS**
- Core recurrence / ICS / conflict logic tests: **PASS**
- Kotlin parser/syntax-pattern scan: **PASS**

## Notification polish

The persistent Now notification no longer uses Android's system chronometer or
promoted ongoing timer presentation.

It now uses compact Dayline text such as:

- `42M LEFT · ENDS 09:00`
- `FOCUS · Research`
- `18M · SESSION 2/5`
- `REST · Research`

A neutral progress bar and a minimal monochrome Dayline notification icon are
used instead. Text is refreshed periodically without showing a seconds timer.

## Final Android compile gate

This execution environment does not contain the Android SDK / Gradle toolchain
needed for the authoritative `:app:assembleDebug` / `:app:bundleRelease` build.

GitHub Actions remains the final Android compile gate after this source is
pushed. Do not create the `v0.12.0` release tag until the main build is green.
