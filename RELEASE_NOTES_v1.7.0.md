# Quran Premium v1.7.0 — Phase 8

- versionCode: 8
- applicationId unchanged: `com.qurankareem.app`
- Added user-controlled JSON backup and restore for reading state, bookmarks/favorites, settings, Hifz, Khatma, Adhkar counters, statistics and saved location.
- Backup format is versioned and fully parsed/validated before local stores are modified.
- Daily-only state is restored only when the backup was created on the current date.
- Hifz screen now displays live verse/repetition/cycle/queue progress and playback controls.
- Home-screen widgets now deep-open the intended in-app destination.
- Added conditional release signing via environment variables; no signing material is stored in source.
- Added manual GitHub Actions signed-AAB workflow plus release verification script and checklist.
- Existing DataStore keys and user data remain backward compatible.
