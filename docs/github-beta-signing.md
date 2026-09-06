# GitHub beta signing

Android only installs a newer APK over an existing Dayline β APK when the package name **and signing certificate** match. Every tagged GitHub beta must therefore use one long-lived beta signing key. Do not generate a new key per release.

## 1. Generate the beta key once

Run locally with JDK 17+:

```bash
keytool -genkeypair -v \
  -keystore dayline-beta.jks \
  -alias dayline-beta \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Store the keystore and passwords somewhere secure outside the repository. Losing this key means existing `com.pix.dayline.beta` installs cannot be updated with a differently signed APK.

## 2. Encode it for GitHub Actions

Linux:

```bash
base64 -w 0 dayline-beta.jks
```

macOS:

```bash
base64 < dayline-beta.jks | tr -d '\n'
```

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("dayline-beta.jks"))
```

## 3. Add the beta Actions secrets

Add these four repository secrets:

- `DAYLINE_BETA_KEYSTORE_BASE64`
- `DAYLINE_BETA_KEYSTORE_PASSWORD`
- `DAYLINE_BETA_KEY_ALIAS`
- `DAYLINE_BETA_KEY_PASSWORD`

`DAYLINE_BETA_KEY_ALIAS` must exactly match the alias used when the keystore was created (for the example above: `dayline-beta`). The workflow now verifies that alias with `keytool` before Gradle runs, so an alias mismatch fails with a clear message.

The Play signing secrets remain separate (`DAYLINE_KEYSTORE_*`). Never reuse or expose either private key in the repository.

## 4. Beta tag flow

After `main` is green:

```bash
git tag v0.13.0.beta
git push origin v0.13.0.beta
```

The tagged workflow builds `:app:assembleBetaRelease`, verifies the APK signature, creates a SHA-256 checksum, attaches both files to a GitHub **prerelease**, and keeps the app ID as `com.pix.dayline.beta`.

## Update migration note

If an older Dayline beta was produced with a different signing key, Android may refuse the first update. Back up Dayline JSON, uninstall the differently signed beta, install the persistently signed beta once, and restore if needed. After that baseline is established, keep the beta key unchanged.
