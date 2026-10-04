package com.qurankareem.feature.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qurankareem.core.audio.AudioCatalogRepository
import com.qurankareem.core.audio.QuranReciter
import com.qurankareem.core.settings.AppSettings
import com.qurankareem.core.settings.PrayerKey
import com.qurankareem.core.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    canScheduleExactAlarms: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onWifiOnlyDownloads: (Boolean) -> Unit,
    onDefaultReciter: (Int, Int) -> Unit,
    onPrayerEnabled: (PrayerKey, Boolean) -> Unit,
    onPrayerReminder: (PrayerKey, Int) -> Unit,
    onAdhanSound: (String, String) -> Unit,
    onPrecisePrayerAlerts: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    backupStatus: String?,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val context = LocalContext.current
    var reciters by remember { mutableStateOf<List<QuranReciter>>(emptyList()) }
    var reciterMenu by remember { mutableStateOf(false) }
    var themeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { AudioCatalogRepository(context.applicationContext).reciters() }
            .onSuccess { reciters = it.filter { r -> r.moshaf.surahNumbers.size == 114 }.ifEmpty { it } }
    }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val picked = result.data?.getParcelableExtra<Uri>(EXTRA_RINGTONE_PICKED_URI)
            if (picked == null) {
                onAdhanSound(SILENT_SOUND, "بدون صوت")
            } else {
                val label = picked.lastPathSegment?.takeIf { it.isNotBlank() } ?: "صوت مختار"
                onAdhanSound(picked.toString(), label)
            }
        }
    }

    fun openRingtonePicker() {
        val current = settings.adhanSoundUri.takeIf { it.isNotBlank() && it != SILENT_SOUND }?.let(Uri::parse)
        val intent = Intent(ACTION_RINGTONE_PICKER).apply {
            putExtra(EXTRA_RINGTONE_TYPE, TYPE_ALARM)
            putExtra(EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(EXTRA_RINGTONE_EXISTING_URI, current)
            putExtra(EXTRA_RINGTONE_TITLE, "اختر صوت الأذان أو التنبيه")
        }
        ringtonePicker.launch(intent)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("الإعدادات") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { SectionTitle("المظهر", Icons.Outlined.Palette) }
            item {
                SettingCard(
                    title = "الثيم",
                    subtitle = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> "حسب إعداد الهاتف"
                        ThemeMode.LIGHT -> "فاتح"
                        ThemeMode.DARK -> "داكن زمردي"
                        ThemeMode.AMOLED -> "أسود AMOLED"
                    },
                ) {
                    Text("تغيير", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { themeMenu = true })
                    DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                        ThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(themeLabel(mode)) },
                                onClick = { onThemeMode(mode); themeMenu = false },
                            )
                        }
                    }
                }
            }

            item { SectionTitle("الصوت والتنزيل", Icons.Outlined.Headphones) }
            item {
                val selectedName = reciters.firstOrNull { it.id == settings.defaultReciterId && it.moshaf.id == settings.defaultMoshafId }?.name
                    ?: if (settings.defaultReciterId == 0) "يُحدد عند أول استخدام" else "القارئ المحفوظ"
                SettingCard("القارئ الافتراضي", selectedName) {
                    Text("اختيار", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { reciterMenu = true })
                    DropdownMenu(expanded = reciterMenu, onDismissRequest = { reciterMenu = false }) {
                        reciters.take(100).forEach { reciter ->
                            DropdownMenuItem(
                                text = { Text(reciter.name) },
                                onClick = {
                                    onDefaultReciter(reciter.id, reciter.moshaf.id)
                                    reciterMenu = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                ToggleSetting(
                    title = "التنزيل عبر Wi-Fi فقط",
                    subtitle = "يمنع تنزيل السور الصوتية عبر بيانات الهاتف",
                    checked = settings.wifiOnlyDownloads,
                    icon = Icons.Outlined.Download,
                    onCheckedChange = onWifiOnlyDownloads,
                )
            }

            item { SectionTitle("الأذان وتنبيهات الصلاة", Icons.Outlined.Notifications) }
            items(PrayerKey.entries, key = { it.name }) { key ->
                PrayerSettingCard(
                    key = key,
                    enabled = settings.prayer(key).enabled,
                    reminderMinutes = settings.prayer(key).reminderMinutes,
                    onEnabled = {
                        if (it) onRequestNotifications()
                        onPrayerEnabled(key, it)
                    },
                    onReminder = { onPrayerReminder(key, it) },
                )
            }
            item {
                SettingCard("صوت الأذان / التنبيه", settings.adhanSoundLabel) {
                    Text("اختيار من الهاتف", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { openRingtonePicker() })
                }
            }
            item {
                ToggleSetting(
                    title = "تنبيهات صلاة بدقة عالية",
                    subtitle = if (canScheduleExactAlarms) "الإذن متاح؛ سيتم استخدام المنبه الدقيق" else "اختياري؛ يحتاج إذن المنبهات الدقيقة من النظام",
                    checked = settings.precisePrayerAlerts,
                    icon = Icons.Outlined.Alarm,
                    onCheckedChange = { enabled ->
                        if (enabled && !canScheduleExactAlarms) onRequestExactAlarm()
                        onPrecisePrayerAlerts(enabled)
                    },
                )
            }

            item { SectionTitle("النسخ الاحتياطي والاستعادة", Icons.Outlined.Save) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("بياناتي", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "احفظ العلامات، آخر قراءة، الختمة، الحفظ، الإحصائيات والإعدادات في ملف JSON يمكنك الاحتفاظ به أو نقله لجهاز آخر. الملفات الصوتية المحملة لا تدخل في النسخة ويمكن تنزيلها من جديد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onCreateBackup, modifier = Modifier.weight(1f)) { Text("إنشاء نسخة") }
                            OutlinedButton(onClick = onRestoreBackup, modifier = Modifier.weight(1f)) { Text("استعادة") }
                        }
                        backupStatus?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { SectionTitle("القراءة والراحة", Icons.Outlined.DarkMode) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("القراءة الهادئة", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "الثيم الداكن وAMOLED يقللان الإضاءة أثناء القراءة الليلية. إعدادات حجم خط المصحف ووضع التركيز متاحة من شاشة القراءة.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 20.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingCard(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun PrayerSettingCard(
    key: PrayerKey,
    enabled: Boolean,
    reminderMinutes: Int,
    onEnabled: (Boolean) -> Unit,
    onReminder: (Int) -> Unit,
) {
    var reminderMenu by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("صلاة ${key.arabicName}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onEnabled)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("التذكير قبل الصلاة", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (reminderMinutes == 0) "بدون تذكير مسبق" else "$reminderMinutes دقيقة",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(enabled = enabled) { reminderMenu = true },
                )
                DropdownMenu(expanded = reminderMenu, onDismissRequest = { reminderMenu = false }) {
                    listOf(0, 5, 10, 15, 30, 45, 60).forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(if (minutes == 0) "بدون تذكير مسبق" else "$minutes دقيقة") },
                            onClick = { onReminder(minutes); reminderMenu = false },
                        )
                    }
                }
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "حسب إعداد الهاتف"
    ThemeMode.LIGHT -> "فاتح"
    ThemeMode.DARK -> "داكن زمردي"
    ThemeMode.AMOLED -> "أسود AMOLED"
}

private const val SILENT_SOUND = "__silent__"
private const val ACTION_RINGTONE_PICKER = "android.intent.action.RINGTONE_PICKER"
private const val EXTRA_RINGTONE_TYPE = "android.intent.extra.ringtone.TYPE"
private const val EXTRA_RINGTONE_SHOW_DEFAULT = "android.intent.extra.ringtone.SHOW_DEFAULT"
private const val EXTRA_RINGTONE_SHOW_SILENT = "android.intent.extra.ringtone.SHOW_SILENT"
private const val EXTRA_RINGTONE_EXISTING_URI = "android.intent.extra.ringtone.EXISTING_URI"
private const val EXTRA_RINGTONE_PICKED_URI = "android.intent.extra.ringtone.PICKED_URI"
private const val EXTRA_RINGTONE_TITLE = "android.intent.extra.ringtone.TITLE"
private const val TYPE_ALARM = 4
