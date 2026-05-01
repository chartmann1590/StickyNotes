# Sticky Notes

Android sticky note editor with a home screen widget, Material 3 UI, Google AdMob (native + banner + interstitial), Firebase (Analytics / Crashlytics / Performance), and GitHub Actions CI.

**Support the project:** [Buy Me a Coffee](https://buymeacoffee.com/charleshartmann)

**Privacy policy (GitHub Pages):** after enabling Pages on `/docs`, set `PRIVACY_POLICY_URL` in `local.properties` to your live policy URL (see [docs/privacy-policy.html](docs/privacy-policy.html)).

## Build

Requirements: JDK 17, Android SDK. Use Android Studio or Gradle:

```bash
./gradlew assembleDebug
```

For signed release builds locally, copy [local.properties.example](local.properties.example) to `local.properties` and fill signing + AdMob keys. Debug builds use Google’s **test** ad unit IDs automatically.

## Repository layout

If `git status` from this folder shows unrelated paths under your user profile, your Git repository root is probably **not** this project. Initialize a dedicated repo inside `StickyNotes` (or open only this folder in Android Studio) before pushing, so CI and secrets apply to the correct remote.

## GitHub Actions

Workflow: [.github/workflows/android.yml](.github/workflows/android.yml)

[![Android CI and Release](https://github.com/YOUR_GITHUB_USER/YOUR_REPO/actions/workflows/android.yml/badge.svg)](https://github.com/YOUR_GITHUB_USER/YOUR_REPO/actions/workflows/android.yml) — replace `YOUR_GITHUB_USER` / `YOUR_REPO` after you publish the project.

- **Unit tests** on every push and PR.
- **Instrumented tests** on an API 34 emulator.
- **Release** (signed APK + AAB + GitHub Release) on push to `main` or `master` only, after tests pass.

### Repository secrets (release job)

| Secret | Purpose |
|--------|---------|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded upload keystore file |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |
| `VERSION_CODE_BASE` | Integer **greater** than max `versionCode` already used in Play Console. CI sets `versionCode = VERSION_CODE_BASE + github.run_number`. |
| `ADMOB_APPLICATION_ID` | AdMob app id (`ca-app-pub-…~…`) |
| `ADMOB_NATIVE_AD_UNIT_ID` | Native advanced unit id |
| `ADMOB_BANNER_AD_UNIT_ID` | Banner unit id |
| `ADMOB_INTERSTITIAL_AD_UNIT_ID` | Interstitial unit id |
| `PRIVACY_POLICY_URL` | Optional; public URL of your privacy policy for release `BuildConfig` |

Never commit keystore files or passwords. Rotate any credential that was exposed.

## GitHub Pages

In the repository **Settings → Pages**, set source to **Deploy from a branch**, branch `main`, folder **`/docs`**. The site will include the support link and privacy policy.

## License

See repository license (if any). Google Play and Firebase services are subject to their respective terms.
