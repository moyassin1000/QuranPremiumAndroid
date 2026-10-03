package com.qurankareem.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qurankareem.core.practice.HifzStage
import com.qurankareem.core.practice.PracticeState
import com.qurankareem.core.stats.AppStats
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(stats: AppStats, practice: PracticeState) {
    val mastered = practice.hifzItems.count { it.stage == HifzStage.MASTERED }
    val hifzTotal = practice.hifzItems.size
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("إحصائياتي", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("ملخص هادئ يساعدك على متابعة القراءة والاستماع والحفظ بدون منافسة أو ضغط.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("صفحات مختلفة", stats.uniquePagesRead.toString(), Modifier.weight(1f))
                StatCard("سلسلة القراءة", "${stats.readingStreak} يوم", Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("الاستماع", "${stats.audioMinutes} د", Modifier.weight(1f))
                StatCard("مراجعات اليوم", practice.dueReviews.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("آخر 7 أيام", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    val today = LocalDate.now().toEpochDay()
                    val counts = stats.recentReading.associate { it.epochDay to it.pages }
                    val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    (6 downTo 0).forEach { offset ->
                        val day = today - offset
                        val date = LocalDate.ofEpochDay(day)
                        val count = counts[day] ?: 0
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ar")), modifier = Modifier.width(52.dp))
                            LinearProgressIndicator(progress = { count / max.toFloat() }, modifier = Modifier.weight(1f))
                            Text("$count", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Text("إجمالي فتحات الصفحات خلال الأسبوع: ${stats.last7DaysPages}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الحفظ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$mastered من $hifzTotal مقطع متقن")
                    LinearProgressIndicator(
                        progress = { if (hifzTotal == 0) 0f else mastered / hifzTotal.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الختمة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (practice.khatma.active) {
                        Text("${practice.khatma.completedCount} من 604 صفحة • ${(practice.khatma.percent * 100).toInt()}%")
                        LinearProgressIndicator(progress = { practice.khatma.percent }, modifier = Modifier.fillMaxWidth())
                        Text("ورد اليوم: ${practice.khatma.todayCount}/${practice.khatma.dailyGoal}")
                    } else {
                        Text("لا توجد ختمة نشطة حاليًا.")
                    }
                }
            }
        }
        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
