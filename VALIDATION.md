# Dayline v0.12.2 — Validation

This patch focuses on widget typography and surface polish.

Checks performed:
- XML resource parsing
- manifest/resource reference checks
- workflow YAML parsing
- Kotlin import/reference sanity checks
- Noto Emoji downloadable-font declaration and preload checks
- verification that no legacy `emoji_*.png` assets remain
- verification that the Pulse `DAYLINE` caption is removed
- verification that widget surfaces use the full neutral system background
- update/full ZIP integrity checks

The Google Noto Emoji font file is **not bundled** with Dayline. Android requests it
from the Google Play Services downloadable-font provider and caches it on the device.

GitHub Actions remains the authoritative full Android/Gradle compile gate.

## v0.12.2 GitHub Actions repair

GitHub Actions reported one Kotlin compilation error:

`DayTimeline.kt:412` attempted to call `.dp` on a `Long`.

The calculation now ends in `.toInt().dp`.

The Node 20 / punycode lines in the uploaded Actions output are post-action
deprecation warnings and are not the cause of the Android build failure.
