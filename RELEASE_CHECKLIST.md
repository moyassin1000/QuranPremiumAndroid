# Quran Premium — Release checklist

## Identity and versioning
- Keep `applicationId = com.qurankareem.app` unchanged after the first Play release.
- Current release: `versionCode = 8`, `versionName = 1.7.0`.
- Increase `versionCode` for every future Play upload.

## Signing
- Never commit a `.jks`, `.keystore`, passwords, or signing secrets.
- GitHub release workflow expects these repository secrets:
  - `ANDROID_KEYSTORE_BASE64`
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`
- Prefer Google Play App Signing for production distribution.

## Before upload
1. Run `python scripts/verify_release.py`.
2. Run `./gradlew :app:testReleaseUnitTest :app:lintRelease :app:bundleRelease`.
3. Install/test the release build on at least one Android 8+ device and one recent Android device.
4. Test: first install, update over previous release, offline Mushaf, audio background playback, prayer notifications, Qibla sensors, backup export/restore, and widgets.
5. Confirm privacy policy and Data safety answers match the actual build.
6. Re-check the current Play Console target API and policy requirements before publishing.

## Update safety
- Never change the production application ID.
- Never rotate the app-signing identity outside the supported Play process.
- Keep DataStore keys backward compatible or migrate them explicitly.
- Restore/backup format is versioned and validates input before modifying local stores.
