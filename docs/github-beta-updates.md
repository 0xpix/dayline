# GitHub beta updates

Dayline now has two distribution flavors:

- `beta` → package `com.pix.dayline.beta`, GitHub Releases update channel enabled.
- `play` → package `com.pix.dayline`, no self-updater permissions; updates come from Google Play.

This keeps the open-source beta channel independent from the stable Play install and lets both apps coexist on one phone.

## First beta signing setup

GitHub beta APKs must always use the same signing key or Android will reject updates.
Generate a dedicated beta keystore locally and keep it private. Do not commit it.

Add these repository Actions secrets:

- `DAYLINE_BETA_KEYSTORE_BASE64`
- `DAYLINE_BETA_KEYSTORE_PASSWORD`
- `DAYLINE_BETA_KEY_ALIAS`
- `DAYLINE_BETA_KEY_PASSWORD`

The existing Play secrets remain separate:

- `DAYLINE_KEYSTORE_BASE64`
- `DAYLINE_KEYSTORE_PASSWORD`
- `DAYLINE_KEY_ALIAS`
- `DAYLINE_KEY_PASSWORD`

## Publishing a beta

Use beta tags, for example:

```text
v0.12.8.beta
```

The GitHub workflow builds `assembleBetaRelease`, creates a SHA-256 checksum and publishes both as a GitHub **prerelease**.

Inside Dayline β, **Settings → Beta updates** checks GitHub Releases, downloads the newest beta APK, validates its checksum and package name, then opens Android's package installer.

On Android 8+, the first in-app installation requires granting **Install unknown apps** to Dayline β. Android shows the system confirmation screen; Dayline never bypasses it.

## Moving from the old pre-flavor beta

Older development APKs used `com.pix.dayline`. The new GitHub beta uses `com.pix.dayline.beta` so it can coexist with the future Play app. If you want your existing data in the new beta, use **Settings → Backup Dayline**, install Dayline β, then **Restore backup**.
