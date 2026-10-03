# بناء APK من GitHub Actions

هذا المشروع مجهز لبناء APK قابل للتثبيت تلقائيًا على GitHub.

## أسرع طريقة
1. أنشئ Repository جديدًا فارغًا على GitHub.
2. ارفع **محتويات هذا المجلد** إلى جذر الـRepository، وليس ملف ZIP نفسه.
3. افتح تبويب **Actions**.
4. اختر **Build Android APK**.
5. اضغط **Run workflow**.
6. بعد نجاح البناء افتح التشغيل، ثم قسم **Artifacts**.
7. حمّل `QuranPremium-v1.7.0-debug-apk`.
8. بعد فك ملف الـArtifact ستجد `app-debug.apk` وهو قابل للتثبيت على Android للاختبار.

## Google Play
Google Play يفضل AAB موقّع. المشروع يحتوي أيضًا على `Build signed release AAB`، لكنه يحتاج أسرار التوقيع الموضحة في `RELEASE_CHECKLIST.md`.

## ثبات هوية التطبيق
- Application ID: `com.qurankareem.app`
- versionCode: `8`
- versionName: `1.7.0`

لا تغيّر Application ID بعد النشر إذا كنت تريد أن تصل الإصدارات الجديدة كتحديث لنفس التطبيق.
