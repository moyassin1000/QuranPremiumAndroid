package com.qurankareem.core.practice

enum class AdhkarCategory(val title: String) {
    MORNING("أذكار الصباح"),
    EVENING("أذكار المساء"),
    AFTER_PRAYER("بعد الصلاة"),
    SLEEP("قبل النوم"),
    WAKE("عند الاستيقاظ"),
}

data class DhikrItem(
    val id: String,
    val category: AdhkarCategory,
    val text: String,
    val target: Int,
    val source: String,
)

object AdhkarData {
    val items: List<DhikrItem> = listOf(
        DhikrItem("morning_kingdom", AdhkarCategory.MORNING, "أصبحنا وأصبح الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير. رب أسألك خير ما في هذا اليوم وخير ما بعده، وأعوذ بك من شر ما في هذا اليوم وشر ما بعده، رب أعوذ بك من الكسل وسوء الكبر، رب أعوذ بك من عذاب في النار وعذاب في القبر.", 1, "صحيح مسلم"),
        DhikrItem("morning_by_you", AdhkarCategory.MORNING, "اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك النشور.", 1, "أبو داود والترمذي"),
        DhikrItem("morning_content", AdhkarCategory.MORNING, "رضيت بالله ربًا، وبالإسلام دينًا، وبمحمد ﷺ نبيًا.", 3, "أبو داود والترمذي"),
        DhikrItem("morning_no_harm", AdhkarCategory.MORNING, "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم.", 3, "أبو داود والترمذي"),
        DhikrItem("morning_tasbih", AdhkarCategory.MORNING, "سبحان الله وبحمده.", 100, "صحيح مسلم"),
        DhikrItem("evening_kingdom", AdhkarCategory.EVENING, "أمسينا وأمسى الملك لله، والحمد لله، لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير. رب أسألك خير ما في هذه الليلة وخير ما بعدها، وأعوذ بك من شر ما في هذه الليلة وشر ما بعدها، رب أعوذ بك من الكسل وسوء الكبر، رب أعوذ بك من عذاب في النار وعذاب في القبر.", 1, "صحيح مسلم"),
        DhikrItem("evening_by_you", AdhkarCategory.EVENING, "اللهم بك أمسينا وبك أصبحنا وبك نحيا وبك نموت وإليك المصير.", 1, "أبو داود والترمذي"),
        DhikrItem("evening_content", AdhkarCategory.EVENING, "رضيت بالله ربًا، وبالإسلام دينًا، وبمحمد ﷺ نبيًا.", 3, "أبو داود والترمذي"),
        DhikrItem("evening_no_harm", AdhkarCategory.EVENING, "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم.", 3, "أبو داود والترمذي"),
        DhikrItem("evening_tasbih", AdhkarCategory.EVENING, "سبحان الله وبحمده.", 100, "صحيح مسلم"),
        DhikrItem("after_astaghfir", AdhkarCategory.AFTER_PRAYER, "أستغفر الله.", 3, "صحيح مسلم"),
        DhikrItem("after_salam", AdhkarCategory.AFTER_PRAYER, "اللهم أنت السلام ومنك السلام، تباركت يا ذا الجلال والإكرام.", 1, "صحيح مسلم"),
        DhikrItem("after_subhan", AdhkarCategory.AFTER_PRAYER, "سبحان الله.", 33, "صحيح مسلم"),
        DhikrItem("after_hamd", AdhkarCategory.AFTER_PRAYER, "الحمد لله.", 33, "صحيح مسلم"),
        DhikrItem("after_akbar", AdhkarCategory.AFTER_PRAYER, "الله أكبر.", 33, "صحيح مسلم"),
        DhikrItem("sleep_name", AdhkarCategory.SLEEP, "باسمك اللهم أموت وأحيا.", 1, "صحيح البخاري"),
        DhikrItem("sleep_subhan", AdhkarCategory.SLEEP, "سبحان الله.", 33, "صحيح البخاري وصحيح مسلم"),
        DhikrItem("sleep_hamd", AdhkarCategory.SLEEP, "الحمد لله.", 33, "صحيح البخاري وصحيح مسلم"),
        DhikrItem("sleep_akbar", AdhkarCategory.SLEEP, "الله أكبر.", 34, "صحيح البخاري وصحيح مسلم"),
        DhikrItem("wake_life", AdhkarCategory.WAKE, "الحمد لله الذي أحيانا بعدما أماتنا وإليه النشور.", 1, "صحيح البخاري"),
    )

    fun forCategory(category: AdhkarCategory): List<DhikrItem> = items.filter { it.category == category }
}
