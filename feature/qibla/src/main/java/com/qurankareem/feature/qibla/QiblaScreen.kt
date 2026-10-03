package com.qurankareem.feature.qibla

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.qurankareem.core.prayer.PrayerCalculator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    latitude: Double,
    longitude: Double,
    locationLabel: String,
    onUseMyLocation: () -> Unit,
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    var heading by remember { mutableFloatStateOf(0f) }
    val qibla = remember(latitude, longitude) { PrayerCalculator.qiblaDirection(latitude, longitude).toFloat() }
    val relative = ((qibla - heading + 540f) % 360f) - 180f
    val aligned = abs(relative) < 4f

    DisposableEffect(sensorManager) {
        val rotation = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            private val matrix = FloatArray(9)
            private val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (rotation != null) sensorManager.registerListener(listener, rotation, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("اتجاه القبلة") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(locationLabel, style = MaterialTheme.typography.titleMedium)
            Text("اتجاه القبلة ${qibla.toInt()}°", style = MaterialTheme.typography.headlineSmall)
            Canvas(Modifier.size(290.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = if (aligned) androidx.compose.ui.graphics.Color(0xFFB78D2B) else androidx.compose.ui.graphics.Color(0xFF1E6A55), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f))
                rotate(relative, pivot = center) {
                    val len = size.minDimension * 0.36f
                    val tip = Offset(center.x, center.y - len)
                    drawLine(color = androidx.compose.ui.graphics.Color(0xFFB78D2B), start = center, end = tip, strokeWidth = 14f)
                    val angle = Math.toRadians(-90.0)
                    val wing = 26f
                    drawLine(color = androidx.compose.ui.graphics.Color(0xFFB78D2B), start = tip, end = Offset(tip.x + (cos(angle + 2.5) * wing).toFloat(), tip.y + (sin(angle + 2.5) * wing).toFloat()), strokeWidth = 10f)
                    drawLine(color = androidx.compose.ui.graphics.Color(0xFFB78D2B), start = tip, end = Offset(tip.x + (cos(angle - 2.5) * wing).toFloat(), tip.y + (sin(angle - 2.5) * wing).toFloat()), strokeWidth = 10f)
                }
            }
            Text(if (aligned) "أنت الآن في اتجاه القبلة" else "حرّك الهاتف حتى يتجه السهم للأعلى", style = MaterialTheme.typography.titleMedium)
            Text("اتجاه الهاتف: ${heading.toInt()}°", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onUseMyLocation) {
                Icon(Icons.Outlined.LocationOn, null)
                Text("تحديث الموقع", modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}
