# Third-party data notices

## Qur'an text (Ḥafṣ)

The reader downloads Qur'an text from Quran.ws and stores the returned text verbatim in local app storage for offline reuse. Phase 5 also offers an on-demand full-text Offline Pack; the project ZIP itself does not bundle the upstream Qur'an text file. Quran.ws documents the Ḥafṣ source as the King Fahd Glorious Qur'an Printing Complex (KFGQPC) Uthmanic Ḥafṣ text. Review the upstream terms before publishing or redistributing bundled copies.

Source: https://text.quran.ws/

## Qur'an structural metadata

Surah names/counts and the 114-surah / 30-juz / 604-page structural model used while developing this phase were checked against Tanzil Qur'an metadata. Tanzil publishes its text/data terms and metadata documentation publicly; retain attribution and do not alter Qur'an text sourced from Tanzil if it is bundled in a later phase.

Metadata documentation: https://tanzil.net/docs/quran_metadata
Text license: https://tanzil.net/docs/Text_License

This project does not claim ownership of Qur'an text or upstream religious data.


## MP3Quran.net
Audio catalog and recitation stream URLs are discovered from MP3Quran.net API v3. Audio remains hosted by MP3Quran servers. Verify redistribution/download terms before publishing offline audio features broadly.

## Adhan2
Prayer-time and Qibla calculations use `com.batoulapps.adhan:adhan2:0.0.7` (MIT License).

## Android alarm / notification behavior
Prayer notifications use Android AlarmManager and notification channels. Exact alarm access is optional and user-controlled through `SCHEDULE_EXACT_ALARM`; the application falls back to `setAndAllowWhileIdle` when exact access is unavailable.


## Adhkar content (Phase 6)

The Phase 6 Adhkar screen contains a small curated starter set of Arabic remembrance texts with short source labels (Sahih al-Bukhari, Sahih Muslim, Abu Dawud, and al-Tirmidhi). It is not presented as an exhaustive hadith database. Before a public production release, perform an independent religious-content review of wording, repetition counts, grading/context, and displayed source attribution.

## EveryAyah verse-by-verse recitations

Phase 7 uses verse-by-verse MP3 URLs from EveryAyah for the Hifz repetition mode. The project references these files remotely and does not bundle the recordings in the repository. Available recitation folders and verse URL structure are documented by EveryAyah (for example `001001.mp3` for surah 1, ayah 1). Verify redistribution/usage terms for any future plan that bundles audio files inside an APK/AAB rather than streaming them from the source.
