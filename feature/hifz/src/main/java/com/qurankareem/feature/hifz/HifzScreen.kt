package com.qurankareem.feature.hifz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qurankareem.core.audio.HifzReciter
import com.qurankareem.core.audio.HifzReciters
import com.qurankareem.core.audio.HifzPlaybackProgress
import com.qurankareem.core.practice.HifzItem
import com.qurankareem.core.practice.HifzStage
import com.qurankareem.core.quran.QuranMetadata
import java.time.LocalDate

@Composable
fun HifzScreen(
    items: List<HifzItem>,
    currentPlayback: HifzPlaybackProgress?,
    playbackQueueIndex: Int,
    playbackQueueSize: Int,
    isPlaying: Boolean,
    onAddPlan: (surah: Int, fromAyah: Int, toAyah: Int, ayahRepeat: Int, rangeRepeat: Int) -> Unit,
    onReview: (key: String, success: Boolean) -> Unit,
    onRemove: (key: String) -> Unit,
    onOpenAyah: (surah: Int, ayah: Int) -> Unit,
    onPlayHifz: (item: HifzItem, reciter: HifzReciter) -> Unit,
    onTogglePlayback: () -> Unit,
    onStopPlayback: () -> Unit,
) {
    var surah by remember { mutableIntStateOf(1) }
    var fromAyah by remember { mutableIntStateOf(1) }
    var toAyah by remember { mutableIntStateOf(7) }
    var ayahRepeat by remember { mutableIntStateOf(5) }
    var rangeRepeat by remember { mutableIntStateOf(3) }
    var reciterId by remember { mutableStateOf(HifzReciters.all.first().id) }
    var reciterMenu by remember { mutableStateOf(false) }
    val reciter = HifzReciters.byId(reciterId)
    val surahInfo = QuranMetadata.surah(surah)
    val due = items.filter { it.isDue && it.stage != HifzStage.MASTERED }
    val mastered = items.count { it.stage == HifzStage.MASTERED }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("الحفظ والمراجعة", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("أنشئ مقاطع للحفظ وكرر كل آية صوتيًا، ثم راجعها على فترات متباعدة.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("قارئ الحفظ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { reciterMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Headphones, contentDescription = null)
                            Text("  ${reciter.name}")
                        }
                        DropdownMenu(expanded = reciterMenu, onDismissRequest = { reciterMenu = false }) {
                            HifzReciters.all.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text(candidate.name) },
                                    onClick = { reciterId = candidate.id; reciterMenu = false },
                                )
                            }
                        }
                    }
                    Text("التكرار الصوتي يستخدم ملفات آية منفصلة لبدء وإنهاء التلاوة بدقة.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (currentPlayback != null) {
            item {
                val currentSurah = QuranMetadata.surah(currentPlayback.surah)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("جلسة الحفظ تعمل الآن", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("سورة ${currentSurah.nameArabic} • الآية ${currentPlayback.ayah}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "تكرار الآية ${currentPlayback.repeatIndex} • دورة المقطع ${currentPlayback.cycle}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val safeTotal = playbackQueueSize.coerceAtLeast(1)
                        val shownIndex = (playbackQueueIndex + 1).coerceIn(1, safeTotal)
                        LinearProgressIndicator(
                            progress = { shownIndex / safeTotal.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("$shownIndex من $safeTotal تشغيل", style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onTogglePlayback, modifier = Modifier.weight(1f)) {
                                Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                                Text(if (isPlaying) "  إيقاف مؤقت" else "  استكمال")
                            }
                            OutlinedButton(onClick = onStopPlayback, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.Stop, contentDescription = null)
                                Text("  إنهاء")
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("مراجعات اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${due.size} مقطع مستحق للمراجعة • $mastered مقطع متقن")
                    val total = items.size.coerceAtLeast(1)
                    LinearProgressIndicator(progress = { mastered / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("إضافة خطة حفظ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    ValueStepper(
                        label = "السورة",
                        value = "$surah — ${surahInfo.nameArabic}",
                        onMinus = {
                            surah = (surah - 1).coerceAtLeast(1)
                            val max = QuranMetadata.surah(surah).ayahCount
                            fromAyah = fromAyah.coerceAtMost(max)
                            toAyah = toAyah.coerceIn(fromAyah, max)
                        },
                        onPlus = {
                            surah = (surah + 1).coerceAtMost(114)
                            val max = QuranMetadata.surah(surah).ayahCount
                            fromAyah = fromAyah.coerceAtMost(max)
                            toAyah = toAyah.coerceIn(fromAyah, max)
                        },
                    )
                    ValueStepper(
                        label = "من الآية",
                        value = fromAyah.toString(),
                        onMinus = { fromAyah = (fromAyah - 1).coerceAtLeast(1); toAyah = toAyah.coerceAtLeast(fromAyah) },
                        onPlus = { fromAyah = (fromAyah + 1).coerceAtMost(surahInfo.ayahCount); toAyah = toAyah.coerceAtLeast(fromAyah) },
                    )
                    ValueStepper(
                        label = "إلى الآية",
                        value = toAyah.toString(),
                        onMinus = { toAyah = (toAyah - 1).coerceAtLeast(fromAyah) },
                        onPlus = { toAyah = (toAyah + 1).coerceAtMost(surahInfo.ayahCount) },
                    )
                    ValueStepper("تكرار كل آية", "$ayahRepeat مرات", { ayahRepeat = (ayahRepeat - 1).coerceAtLeast(1) }, { ayahRepeat = (ayahRepeat + 1).coerceAtMost(20) })
                    ValueStepper("تكرار المقطع", "$rangeRepeat مرات", { rangeRepeat = (rangeRepeat - 1).coerceAtLeast(1) }, { rangeRepeat = (rangeRepeat + 1).coerceAtMost(20) })
                    Button(onClick = { onAddPlan(surah, fromAyah, toAyah, ayahRepeat, rangeRepeat) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                        Text("  إضافة لخطة الحفظ")
                    }
                }
            }
        }

        if (due.isNotEmpty()) {
            item { Text("مستحق للمراجعة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(due, key = { "due-${it.key}" }) { item ->
                HifzItemCard(item, reciter, onReview, onRemove, onOpenAyah, onPlayHifz)
            }
        }

        item { Text("كل خطة الحفظ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (items.isEmpty()) {
            item { Card(Modifier.fillMaxWidth()) { Text("لا توجد مقاطع محفوظة بعد. أضف أول مقطع من الأعلى.", Modifier.padding(16.dp)) } }
        } else {
            items(items, key = { "all-${it.key}" }) { item ->
                HifzItemCard(item, reciter, onReview, onRemove, onOpenAyah, onPlayHifz)
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}

@Composable
private fun ValueStepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Row {
            OutlinedButton(onClick = onMinus) { Text("−") }
            Spacer(Modifier.padding(horizontal = 3.dp))
            OutlinedButton(onClick = onPlus) { Text("+") }
        }
    }
}

@Composable
private fun HifzItemCard(
    item: HifzItem,
    reciter: HifzReciter,
    onReview: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
    onOpenAyah: (Int, Int) -> Unit,
    onPlayHifz: (HifzItem, HifzReciter) -> Unit,
) {
    val surah = QuranMetadata.surah(item.surah)
    val reviewDate = LocalDate.ofEpochDay(item.nextReviewEpochDay)
    val queueSize = (item.toAyah - item.fromAyah + 1).coerceAtLeast(1) * item.ayahRepeat * item.rangeRepeat
    val playable = queueSize <= 1000
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("سورة ${surah.nameArabic}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("الآيات ${item.fromAyah}–${item.toAyah} • ${stageLabel(item.stage)}")
                }
                IconButton(onClick = { onRemove(item.key) }) { Icon(Icons.Outlined.Delete, contentDescription = "حذف") }
            }
            Text("التكرار: الآية ×${item.ayahRepeat} • المقطع ×${item.rangeRepeat}", style = MaterialTheme.typography.bodySmall)
            Text(
                if (item.stage == HifzStage.MASTERED) "تم الوصول لمرحلة الإتقان" else "المراجعة: $reviewDate${if (item.isDue) " • مستحقة الآن" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isDue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { onOpenAyah(item.surah, item.fromAyah) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null)
                    Text(" فتح")
                }
                Button(onClick = { onPlayHifz(item, reciter) }, enabled = playable, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Headphones, contentDescription = null)
                    Text(" تكرار صوتي")
                }
            }
            if (!playable) {
                Text("المقطع كبير جدًا للتكرار دفعة واحدة؛ قسّمه إلى خطط أصغر حتى 1000 تشغيل.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            if (item.stage != HifzStage.MASTERED) {
                Button(onClick = { onReview(item.key, true) }, modifier = Modifier.fillMaxWidth()) { Text("مراجعة ناجحة") }
                TextButton(onClick = { onReview(item.key, false) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text("  يحتاج تثبيت — راجعه غدًا")
                }
            }
        }
    }
}

private fun stageLabel(stage: HifzStage): String = when (stage) {
    HifzStage.LEARNING -> "قيد الحفظ"
    HifzStage.REVIEW -> "مراجعة"
    HifzStage.MASTERED -> "متقن"
}
