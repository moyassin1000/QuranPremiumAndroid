package com.qurankareem.feature.khatma

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qurankareem.core.practice.KhatmaProgress
import java.time.LocalDate

@Composable
fun KhatmaScreen(
    progress: KhatmaProgress,
    onStart: (days: Int) -> Unit,
    onStop: () -> Unit,
    onOpenPage: (page: Int) -> Unit,
) {
    var selectedDays by remember { mutableIntStateOf(if (progress.active) progress.goalDays else 30) }
    val presets = listOf(7, 10, 15, 30, 60, 90)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text("الختمة والورد اليومي", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("تقدم بسيط وواضح مبني على الصفحات التي تقرؤها داخل المصحف.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (!progress.active) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ابدأ ختمة جديدة", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("اختر المدة التي تناسبك، وسيحسب التطبيق وردك اليومي تلقائيًا.")
                        presets.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { days ->
                                    if (days == selectedDays) {
                                        Button(onClick = { selectedDays = days }, modifier = Modifier.weight(1f)) { Text("$days يوم") }
                                    } else {
                                        OutlinedButton(onClick = { selectedDays = days }, modifier = Modifier.weight(1f)) { Text("$days يوم") }
                                    }
                                }
                            }
                        }
                        Text("مدة مخصصة", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedDays = (selectedDays - 1).coerceAtLeast(1) }, modifier = Modifier.weight(1f)) { Text("−") }
                            OutlinedButton(onClick = { }, modifier = Modifier.weight(2f), enabled = false) { Text("$selectedDays يوم") }
                            OutlinedButton(onClick = { selectedDays = (selectedDays + 1).coerceAtMost(365) }, modifier = Modifier.weight(1f)) { Text("+") }
                        }
                        val daily = kotlin.math.ceil(604.0 / selectedDays).toInt()
                        Text("ورد مقترح: $daily صفحة يوميًا • نحو ${kotlin.math.ceil(daily / 5.0).toInt()} صفحات بعد كل صلاة")
                        Button(onClick = { onStart(selectedDays) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                            Text("  بدء الختمة")
                        }
                    }
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ختمة ${progress.goalDays} يومًا", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${progress.completedCount} من 604 صفحة")
                        LinearProgressIndicator(progress = { progress.percent }, modifier = Modifier.fillMaxWidth())
                        Text("${(progress.percent * 100).toInt()}% مكتمل • المتبقي ${progress.remainingPages} صفحة")
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ورد اليوم", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${progress.todayCount} من ${progress.dailyGoal} صفحة")
                        LinearProgressIndicator(
                            progress = { (progress.todayCount / progress.dailyGoal.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (progress.todayRemaining == 0) {
                            Text("أتممت ورد اليوم ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        } else {
                            Text("متبقي ${progress.todayRemaining} صفحة اليوم • الهدف بعد كل صلاة: ${progress.perPrayerGoal} صفحات")
                        }
                        if (progress.remainingPages == 0) {
                            Text("أتممت الختمة كاملة ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        } else {
                            Button(onClick = { onOpenPage(progress.nextPage) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.MenuBook, contentDescription = null)
                                Text("  متابعة من الصفحة ${progress.nextPage}")
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("تفاصيل الخطة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (progress.startedEpochDay > 0) {
                            val start = LocalDate.ofEpochDay(progress.startedEpochDay)
                            Text("بدأت: $start")
                            Text("الموعد المستهدف: ${start.plusDays(progress.goalDays.toLong())}")
                        }
                        Text("تُحسب الصفحة مرة واحدة فقط حتى لو فتحتها أكثر من مرة، وورد اليوم يُعاد تلقائيًا مع بداية يوم جديد.")
                    }
                }
            }
            item {
                TextButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("إيقاف الختمة الحالية") }
            }
        }
        item { Spacer(Modifier.height(90.dp)) }
    }
}
