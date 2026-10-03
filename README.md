# Quran Premium Android — GitHub APK Build

> المشروع مجهز لبناء APK تلقائيًا من GitHub Actions. راجع `GITHUB_APK_BUILD.md`.

# Quran Premium Android

مشروع تطبيق قرآن كريم احترافي باستخدام Kotlin وJetpack Compose، بتصميم عربي RTL وبنية قابلة للتوسع والنشر على Google Play.

## الحالة الحالية — v1.7.0 / Phase 8

تم تنفيذ الأساس السابق وإضافة أول محرك قراءة فعلي:

- RTL عربي مفروض داخل Compose.
- Design System أخضر زمردي / ذهبي / عاجي.
- Bottom Navigation: الرئيسية، المصحف، الاستماع، القبلة، المزيد.
- Home Dashboard مع حفظ آخر صفحة قراءة.
- تبويب مصحف كامل يتضمن:
  - قائمة 114 سورة مع عدد الآيات.
  - قائمة 30 جزءًا.
  - شبكة 604 صفحة.
  - بحث باسم السورة.
- قارئ قرآن فعلي يدعم القراءة حسب:
  - الصفحة.
  - السورة.
  - الجزء.
- مصدر النص الحالي: Quran.ws / KFGQPC Hafs text.
- يمكن تنزيل حزمة النص القرآني كاملة مرة واحدة؛ بعدها تعمل الصفحات والسور والأجزاء والبحث Offline.
- البحث داخل الآيات يدعم مطابقة عربية مبسطة بدون الاعتماد على التشكيل.
- حفظ آخر صفحة عبر DataStore.
- Bookmark وFavorite للصفحات مع الحفاظ على توافق بيانات النسخ السابقة.
- Bookmark ومفضلة على مستوى الآية نفسها.
- قسم «المحفوظات» لفتح الآيات المحفوظة مباشرة.
- الانتقال من نتيجة البحث مباشرة إلى الآية مع تمييزها.
- وضع تركيز للقراءة.
- تغيير حجم النص من داخل القارئ.
- تحسين Typography القرآن باستخدام Arabic serif system fallback وارتفاع سطر أكبر للقراءة الطويلة.
- السابق / التالي في وضع الصفحات.
- حالات Loading / Offline / Error واضحة.

## Modules

- `app`
- `core:design`
- `core:quran`
- `feature:home`
- `feature:mushaf`
- `core:audio`
- `core:prayer`
- `core:settings`
- `core:practice`
- `feature:audio`
- `feature:prayer`
- `feature:qibla`
- `feature:settings`
- `feature:hifz`
- `feature:khatma`
- `feature:adhkar`
- `core:stats`
- `feature:stats`

## Quran data policy

لا يتم إنشاء أو تعديل النص القرآني داخل التطبيق.

مصدر النص في هذه المرحلة:

- https://text.quran.ws/
- Ḥafṣ عن ʿĀṣim.
- المصدر الأصلي المعلن من الخدمة: King Fahd Glorious Qur'an Printing Complex (KFGQPC).

النص الذي تم تحميله من المصدر يحفظ verbatim داخل التخزين المحلي الخاص بالتطبيق ثم تتم قراءته كما هو.

راجع أيضًا `THIRD_PARTY_NOTICES.md` قبل النشر العام أو إعادة توزيع أي بيانات مضمّنة.

> لا يتم تضمين ملف خط خارجي في الحزمة الحالية. القارئ يستخدم `FontFamily.Serif` المتاح من النظام، مع بنية جاهزة لتطوير Typography لاحقًا دون تغيير بيانات القرآن.

## Google Play Updates

- `applicationId = com.qurankareem.app`
- `versionCode = 8`
- `versionName = 1.7.0`
- `targetSdk = 36`
- `compileSdk = 37`

مهم: لا تغيّر `applicationId` بعد أول نشر على Google Play إذا كنت تريد أن تصل النسخ الجديدة كتحديث لنفس التطبيق.

## البناء

المشروع يستخدم AGP 9.4.1 وGradle 9.6.0 وJDK 17+.

ثبت Android SDK Platform 37 ثم:

```bash
./gradlew :app:assembleDebug
```

ولإنشاء Android App Bundle:

```bash
./gradlew :app:bundleRelease
```

## المرحلة التالية

1. Room + migrations عند انتقال البيانات المركبة إلى قاعدة محلية أكبر.
2. اختبارات Android instrumentation تشمل التحديث والاستعادة والـWidgets.
3. استيراد/تصدير اختياري لخطة الحفظ والختمة بشكل منفصل.
4. تحسين التلاوة آية بآية مع تنزيل Hifz Offline.
5. تجهيز صفحة الخصوصية وData Safety النهائية قبل Google Play Production.

## الأمان

- لا API keys داخل GitHub.
- لا keystore أو signing passwords داخل المستودع.
- `.gitignore` يستبعد الأسرار وملفات التوقيع.
- لا يتم توليد أو إعادة صياغة آيات القرآن باستخدام AI.


## Phase 3 — Audio, Prayer & Qibla (v1.2.0)

- MP3Quran API v3 reciter catalog with Arabic names and per-reciter surah availability.
- Media3 `MediaSessionService` background playback with lock-screen / notification controls.
- Persistent mini-player in app navigation.
- WorkManager offline surah download path.
- Prayer times calculated locally using Adhan2 + Egyptian calculation method.
- Qibla direction calculated locally with device rotation-vector sensor.
- Location remains optional: Cairo is a safe default until the user grants location permission.
- `versionCode=3`, `versionName=1.2.0`; `applicationId` remains `com.qurankareem.app`.

## Phase 4 — v1.3.0

الإصدار 1.3.0 يضيف طبقة التخصيص والتنبيهات المتقدمة:

- ثيم فاتح، داكن زمردي، AMOLED، أو اتباع النظام.
- إعداد قارئ افتراضي وحفظه محليًا.
- خيار تنزيل السور عبر Wi‑Fi فقط.
- إدارة الملفات الصوتية المحملة وعرض الحجم وحذف ملف أو حذف الكل.
- سرعة تشغيل 0.75x إلى 2x.
- تكرار السورة الحالية عبر Media3.
- مؤقت نوم يوقف التلاوة تلقائيًا.
- إعداد مستقل لتنبيه الفجر والظهر والعصر والمغرب والعشاء.
- تذكير قبل كل صلاة 5/10/15/30/45/60 دقيقة أو بدون تذكير مسبق.
- اختيار صوت الأذان/التنبيه من الأصوات المتاحة على الجهاز.
- جدولة تنبيهات الصلاة مع دعم Doze؛ الوضع الافتراضي لا يحتاج exact-alarm access.
- خيار دقة عالية يستخدم SCHEDULE_EXACT_ALARM فقط بعد منح المستخدم الإذن.
- إعادة جدولة تنبيهات الصلاة بعد إعادة تشغيل الجهاز أو تحديث التطبيق.

> تنبيهات الصلاة تكون متوقفة افتراضيًا حتى يفعّلها المستخدم ويمنح إذن الإشعارات عند الحاجة.

## Phase 5 — v1.4.0

- Full Qur'an text Offline Pack downloaded on demand from Quran.ws and retained in app-private storage.
- Offline pack becomes the fallback source for page, surah and juz readers, avoiding repeated network calls.
- Qur'an-wide Arabic search with normalization that ignores common diacritic/orthographic differences.
- Search results include surah, ayah and 604-page location and open directly on the target ayah.
- Ayah-level bookmarks and favorites stored in DataStore without removing legacy page-level data.
- Saved-ayahs tab for quick navigation.
- 604 verified page-start references and 30 juz-start references are built into metadata for deterministic offline slicing and page lookup.
- Reader typography upgraded to system Serif with larger line height and target-ayah highlighting.
- `applicationId` remains `com.qurankareem.app`; `versionCode=5`, `versionName=1.4.0`.

## Phase 6 — v1.5.0

- نظام Hifz حقيقي لإنشاء مقاطع حفظ حسب السورة ونطاق الآيات.
- تخزين عدد تكرارات الآية والمقطع لكل خطة حفظ استعدادًا لطبقة التكرار الصوتي آية بآية.
- مراجعة متباعدة محفوظة محليًا، مع قائمة مراجعات مستحقة اليوم وحالات: قيد الحفظ / مراجعة / متقن.
- Khatma Planner لخطط 7/10/15/30/60/90 يومًا.
- احتساب تلقائي للورد اليومي والهدف التقريبي بعد كل صلاة.
- الصفحات المفتوحة في قارئ الصفحات تُسجّل مرة واحدة فقط ضمن الختمة، ولا تتضاعف عند إعادة فتح الصفحة.
- ورد اليوم يتجدد تلقائيًا مع اليوم الجديد مع بقاء تقدم الختمة الكلي.
- شاشة أذكار بوضع تركيز وعداد يومي محفوظ للفئات: الصباح، المساء، بعد الصلاة، قبل النوم، والاستيقاظ.
- مجموعة أذكار أولية من نصوص مشهورة من صحيح البخاري وصحيح مسلم وأبي داود والترمذي، مع إظهار مرجع مختصر لكل ذكر داخل الواجهة.
- الصفحة الرئيسية تعرض تقدم الورد وعدد مراجعات الحفظ المستحقة، مع وصول سريع للحفظ والأذكار.
- جميع بيانات المرحلة محفوظة في DataStore ولا تمسح بيانات المصحف أو الإعدادات الموجودة من الإصدارات السابقة.
- `applicationId` يبقى `com.qurankareem.app`; `versionCode=6`, `versionName=1.5.0`.

> مجموعة الأذكار في هذه المرحلة بداية منسقة وليست موسوعة كاملة. قبل النشر العام النهائي يُنصح بإجراء مراجعة شرعية مستقلة للنصوص والمراجع وإضافة سياسة واضحة لمصدر المحتوى الديني.


## Phase 7 — v1.6.0

- تكرار صوتي آية بآية داخل Hifz مع أربعة قراء وخطة finite حسب عدد تكرار الآية والمقطع.
- شاشة إحصائيات للقراءة والاستماع والحفظ والختمة وسلسلة القراءة وآخر 7 أيام.
- ثلاثة Android Home Screen Widgets: الصلاة القادمة، متابعة القراءة، وورد اليوم.
- مدير تنزيلات يعرض queued/downloading/failed، نسبة التقدم والحجم والإلغاء.
- استكمال تنزيلات `.part` باستخدام HTTP Range عندما يدعمه الخادم.
- `applicationId` ثابت؛ `versionCode=7`, `versionName=1.6.0`.


## Phase 8 — v1.7.0

- Backup يدوي بصيغة JSON من داخل الإعدادات للعلامات، آخر قراءة، الحفظ، الختمة، الأذكار، الإحصائيات، إعدادات التطبيق والموقع المحفوظ.
- الاستعادة تتحقق من `format` و`schemaVersion` وتقوم بتحليل الملف كاملًا قبل تعديل أي DataStore.
- العدادات اليومية للأذكار وورد اليوم لا تُنقل إلى يوم مختلف عند استعادة نسخة قديمة، بينما يبقى تقدم الختمة الكلي محفوظًا.
- جلسة Hifz تعرض الآية الحالية، رقم تكرار الآية، دورة المقطع والتقدم الكلي في قائمة التشغيل مع Pause/Resume/Stop.
- Widgets أصبحت اختصارات مباشرة: الصلاة → مواقيت الصلاة، القراءة → آخر صفحة، الورد → الختمة.
- مسار GitHub Actions جديد لبناء AAB موقّع يدويًا من Secrets بدون رفع keystore للمستودع.
- `scripts/verify_release.py` يفحص applicationId، الإصدار، targetSdk ووجود ملفات توقيع/Private Keys قبل Release.
- ملف `RELEASE_CHECKLIST.md` يوثق خطوات الاختبار والتوقيع والتحديث قبل رفع Google Play.
- `applicationId` ثابت؛ `versionCode=8`, `versionName=1.7.0`.
