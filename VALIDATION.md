# Dayline v0.12.1-beta.2 — Validation Report

## Release checks completed

## CI repair in beta.2

- Settings widget arguments have safe defaults, so callers from the previous source shape compile.
- Emoji preview uses a local resource mapper instead of the unresolved `iconRes` extension.
- Font labels use `FontChoice.name`, so additional enum entries remain exhaustive-safe.
- GitHub Actions use Node 24-native majors.
- Pages deployment is conditional until Pages is enabled in repository settings.


- Static release validator: **PASS**
- XML resources across main/beta/play source sets: **PASS**
- GitHub Actions workflow YAML: **PASS**
- Distribution flavor separation: **PASS**
- GitHub beta-only `INTERNET` + `REQUEST_INSTALL_PACKAGES`: **PASS**
- Play/main manifest remains self-updater-permission free: **PASS**
- Beta updater verifies SHA-256, package name, and a newer version code before install: **PASS**
- GitHub beta workflow uses a dedicated signing-key secret set: **PASS**
- GitHub beta workflow publishes APK + checksum as a prerelease: **PASS**
- Play workflow explicitly builds `assemblePlayRelease` + `bundlePlayRelease`: **PASS**
- Android automatic backup disabled: **PASS**
- Notification chronometer / `HH:mm:ss` path remains removed: **PASS**

## Distribution design

- `beta`: `com.pix.dayline.beta`, app label **Dayline β**, GitHub update UI enabled.
- `play`: `com.pix.dayline`, GitHub update UI disabled and no updater permissions.

The separate package IDs let a beta tester keep Dayline β next to the stable Google Play app without signature/update-channel conflicts.

## Beta update flow

Settings opens with a lightweight GitHub beta-release check. When a newer prerelease exists, the user can download it. The updater checks the published SHA-256 checksum, verifies that the APK package is the current beta package and that its version code is newer, then opens Android's official package installer. Installation is never silent.

## Final Android compile gate

This execution environment has Kotlin/JVM but no Android SDK/Gradle installation, so it cannot perform the authoritative `assembleBetaDebug`, `assemblePlayDebug`, `assembleBetaRelease`, or `bundlePlayRelease` build. GitHub Actions remains the final Android compile gate.
