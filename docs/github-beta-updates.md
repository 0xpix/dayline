# GitHub beta updates

Dayline now has two distribution flavors:

- `beta` → package `com.pix.dayline.beta`, GitHub Releases update channel enabled.
- `play` → package `com.pix.dayline`, no self-updater permissions; updates come from Google Play.

This keeps the open-source beta channel independent from the stable Play install and lets both apps coexist on one phone.

The updater is anonymous and intentionally contains no GitHub token, so the repository/releases must be publicly readable for update checks to work.

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

Do not create release tags manually. Bump the beta version/release notes and merge or push the finished change to `main`.

After main-branch CI passes, the GitHub workflow automatically creates the matching beta tag, builds `assembleBetaRelease`, verifies the signed APK version/signature, creates a SHA-256 checksum, and publishes the APK/checksum as a GitHub **prerelease**.

Inside Dayline β, **Settings → Beta updates** checks the GitHub Releases list, compares the installed version with published betas, downloads only the newest compatible APK, validates its checksum/package/version, then opens Android's package installer.

**What's new** keeps every exact release between the installed version and the target version instead of collapsing skipped betas into the newest release. **Settings → Beta updates → Changelog** exposes the release history at any time. Starting with v0.18.9.beta, Dayline also remembers the last launched version and shows the exact missed release notes once after a successful app upgrade; if history loading fails, the pending from-version is retained and retried on the next launch.

On Android 8+, the first in-app installation requires granting **Install unknown apps** to Dayline β. Android shows the system confirmation screen; Dayline never bypasses it.

## Moving from the old pre-flavor beta

Older development APKs used `com.pix.dayline`. The new GitHub beta uses `com.pix.dayline.beta` so it can coexist with the future Play app. If you want your existing data in the new beta, use **Settings → Backup Dayline**, install Dayline β, then **Restore backup**.
