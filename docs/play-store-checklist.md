# Dayline Google Play Release Checklist

## Account / listing
- Verify the Play Console developer account.
- Make the GitHub repository public before using it as the public source-code/privacy-policy location.
- Enable GitHub Pages with GitHub Actions. This repository includes `pages.yml`, which publishes `docs/privacy-policy.html` at `https://0xpix.github.io/dayline/privacy-policy.html` after the repository is public. Before Play submission, replace the policy contact/developer identity as needed so it matches the Play Console listing.
- Complete App content, Data safety, content rating, target audience, ads declaration, and store-contact fields.
- Prepare a 512×512 icon, 1024×500 feature graphic, and phone screenshots.

## Signing
Create one upload key locally and keep it private. Never commit it.

Example:

```bash
keytool -genkeypair -v \
  -keystore dayline-upload.jks \
  -alias dayline-upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Add these GitHub Actions secrets:
- `DAYLINE_KEYSTORE_BASE64`
- `DAYLINE_KEYSTORE_PASSWORD`
- `DAYLINE_KEY_ALIAS`
- `DAYLINE_KEY_PASSWORD`

Create `DAYLINE_KEYSTORE_BASE64` from the binary keystore without changing it. On Linux/macOS:

```bash
base64 -w 0 dayline-upload.jks
```

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("dayline-upload.jks"))
```

## Build
- Main pushes continue to produce the open-source debug APK through `build-apk.yml`.
- Run the `Build Play Release` workflow manually after configuring signing secrets.
- Download `dayline-play-release`, containing the signed `.aab` and release `.apk`.
- Upload the `.aab` to Play Console internal testing first.

## Release QA
- Fresh install on supported Android versions.
- Upgrade from previous Dayline build without data loss.
- Permission denied/granted flows.
- Android Calendar import/write/edit behavior.
- Recurring event scopes.
- Widgets in light/dark/system backgrounds.
- Notifications / focus actions / exact-alarm fallback.
- Backup → uninstall → reinstall → restore.
- ICS import/export round-trip.
- TalkBack/basic accessibility and large-font check.
- No signing key or secrets in git history/artifacts.
