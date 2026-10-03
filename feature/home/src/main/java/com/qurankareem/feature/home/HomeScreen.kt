package com.qurankareem.feature.home

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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qurankareem.core.design.PremiumCard

@Composable
fun HomeScreen(
    lastPage: Int,
    nextPrayerName: String,
    nextPrayerTime: String,
    locationLabel: String,
    khatmaActive: Boolean,
    wirdDone: Int,
    wirdGoal: Int,
    dueReviews: Int,
    onContinueReading: () -> Unit,
    onOpenMushaf: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenKhatma: () -> Unit,
    onOpenHifz: () -> Unit,
    onOpenAdhkar: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text("السلام عليكم", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("مرحبًا بك في رفيقك اليومي للقرآن والصلاة", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
        }

        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("الصلاة القادمة", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(nextPrayerName, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(nextPrayerTime)
                        Text(locationLabel, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Outlined.Mosque, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = { 0.5f })
            }
        }

        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("واصل قراءتك", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(6.dp))
                Text("آخر موضع محفوظ: الصفحة $lastPage")
                Spacer(Modifier.height(12.dp))
                Button(onClick = onContinueReading, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null)
                    Text("  متابعة القراءة")
                }
                androidx.compose.material3.TextButton(onClick = onOpenMushaf, modifier = Modifier.fillMaxWidth()) {
                    Text("تصفح السور والأجزاء والصفحات")
                }
            }
        }

        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("ورد اليوم", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(if (khatmaActive) "$wirdDone من $wirdGoal صفحات" else "ابدأ خطة ختمة لتفعيل الورد")
                    }
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { if (khatmaActive && wirdGoal > 0) (wirdDone / wirdGoal.toFloat()).coerceIn(0f, 1f) else 0f },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenKhatma, modifier = Modifier.fillMaxWidth()) {
                    Text(if (khatmaActive) "عرض الختمة والورد" else "إنشاء خطة ختمة")
                }
            }
        }

        if (dueReviews > 0) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("لديك $dueReviews مراجعة حفظ مستحقة اليوم", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenHifz, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.School, contentDescription = null)
                        Text("  ابدأ المراجعة")
                    }
                }
            }
        }

        item {
            Text("الوصول السريع", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpenAudio, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Headphones, contentDescription = null)
                    Text(" استماع")
                }
                Button(onClick = onOpenQibla, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Navigation, contentDescription = null)
                    Text(" القبلة")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenHifz, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.School, contentDescription = null)
                    Text(" الحفظ")
                }
                OutlinedButton(onClick = onOpenAdhkar, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = null)
                    Text(" الأذكار")
                }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpenPrayer, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Mosque, contentDescription = null)
                Text(" مواقيت الصلاة وإعدادات الأذان")
            }
            Spacer(Modifier.height(100.dp))
        }
    }
}
