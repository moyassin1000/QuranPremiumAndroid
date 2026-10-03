package com.qurankareem.feature.adhkar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qurankareem.core.practice.AdhkarCategory
import com.qurankareem.core.practice.AdhkarData

@Composable
fun AdhkarScreen(
    counts: Map<String, Int>,
    onIncrement: (id: String, target: Int) -> Unit,
    onReset: (id: String) -> Unit,
) {
    var categoryIndex by remember { mutableIntStateOf(0) }
    var itemIndex by remember { mutableIntStateOf(0) }
    val categories = AdhkarCategory.entries
    val category = categories[categoryIndex]
    val items = AdhkarData.forCategory(category)
    val item = items[itemIndex.coerceIn(0, items.lastIndex)]
    val current = (counts[item.id] ?: 0).coerceAtMost(item.target)
    val completed = items.count { (counts[it.id] ?: 0) >= it.target }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
            Text("الأذكار", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("وضع تركيز: ذكر واحد في كل مرة، مع عداد يومي محفوظ.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ScrollableTabRow(selectedTabIndex = categoryIndex, edgePadding = 12.dp) {
            categories.forEachIndexed { index, c ->
                Tab(
                    selected = index == categoryIndex,
                    onClick = { categoryIndex = index; itemIndex = 0 },
                    text = { Text(c.title) },
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${itemIndex + 1} / ${items.size}")
                Text("مكتمل $completed / ${items.size}")
            }
            LinearProgressIndicator(progress = { completed / items.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(22.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        item.text,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 25.sp,
                        lineHeight = 43.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(item.source, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(
                onClick = { onIncrement(item.id, item.target) },
                modifier = Modifier.fillMaxWidth(),
                enabled = current < item.target,
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                Text(if (current >= item.target) "  اكتمل الذكر" else "  $current / ${item.target} — اضغط للعد")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { itemIndex = (itemIndex - 1).coerceAtLeast(0) },
                    modifier = Modifier.weight(1f),
                    enabled = itemIndex > 0,
                ) { Text("السابق") }
                OutlinedButton(
                    onClick = { itemIndex = (itemIndex + 1).coerceAtMost(items.lastIndex) },
                    modifier = Modifier.weight(1f),
                    enabled = itemIndex < items.lastIndex,
                ) { Text("التالي") }
            }
            TextButton(onClick = { onReset(item.id) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
                Text("  إعادة عد هذا الذكر")
            }
        }
    }
}
