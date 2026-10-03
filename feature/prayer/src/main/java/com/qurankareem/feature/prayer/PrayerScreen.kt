package com.qurankareem.feature.prayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qurankareem.core.prayer.PrayerCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    latitude: Double,
    longitude: Double,
    locationLabel: String,
    onUseMyLocation: () -> Unit,
) {
    val today = java.time.LocalDate.now()
    val day = remember(latitude, longitude, today) { PrayerCalculator.calculate(latitude, longitude, today) }
    val now = System.currentTimeMillis()
    val next = day.all.firstOrNull { it.name != "الشروق" && it.epochMillis > now }
    Scaffold(topBar = { TopAppBar(title = { Text("مواقيت الصلاة") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(locationLabel, style = MaterialTheme.typography.titleLarge)
                    Text("طريقة الحساب: الهيئة المصرية العامة للمساحة", style = MaterialTheme.typography.bodyMedium)
                    if (next != null) {
                        Text("الصلاة القادمة: ${next.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
                        Text(PrayerCalculator.format(next.epochMillis), style = MaterialTheme.typography.headlineMedium)
                    }
                    Button(onClick = onUseMyLocation, modifier = Modifier.padding(top = 10.dp)) {
                        Icon(Icons.Outlined.LocationOn, null)
                        Text("استخدام موقعي", modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
            day.all.forEach { prayer ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(prayer.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(PrayerCalculator.format(prayer.epochMillis), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
