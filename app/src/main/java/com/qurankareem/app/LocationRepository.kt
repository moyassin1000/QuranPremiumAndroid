package com.qurankareem.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

data class AppLocation(
    val latitude: Double = 30.0444,
    val longitude: Double = 31.2357,
    val label: String = "القاهرة - الموقع الافتراضي",
)

class LocationRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)

    fun saved(): AppLocation = AppLocation(
        latitude = Double.fromBits(prefs.getLong("lat", 30.0444.toBits())),
        longitude = Double.fromBits(prefs.getLong("lon", 31.2357.toBits())),
        label = prefs.getString("label", "القاهرة - الموقع الافتراضي") ?: "القاهرة - الموقع الافتراضي",
    )

    fun save(location: AppLocation) {
        val safeLat = location.latitude.coerceIn(-90.0, 90.0)
        val safeLon = location.longitude.coerceIn(-180.0, 180.0)
        prefs.edit()
            .putLong("lat", safeLat.toBits())
            .putLong("lon", safeLon.toBits())
            .putString("label", location.label.ifBlank { "موقع محفوظ" })
            .apply()
    }

    fun bestLastKnown(): AppLocation? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null
        val manager = context.getSystemService(LocationManager::class.java)
        val locations = manager.getProviders(true).mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        val best = locations.maxByOrNull { it.time } ?: return null
        return AppLocation(best.latitude, best.longitude, "موقعي الحالي").also {
            prefs.edit()
                .putLong("lat", it.latitude.toBits())
                .putLong("lon", it.longitude.toBits())
                .putString("label", it.label)
                .apply()
        }
    }
}
