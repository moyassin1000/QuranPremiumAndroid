package com.qurankareem.core.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.data.DateComponents
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.Qibla
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PrayerMoment(val name: String, val epochMillis: Long)

data class PrayerDay(
    val fajr: PrayerMoment,
    val sunrise: PrayerMoment,
    val dhuhr: PrayerMoment,
    val asr: PrayerMoment,
    val maghrib: PrayerMoment,
    val isha: PrayerMoment,
) {
    val all: List<PrayerMoment> get() = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
}

object PrayerCalculator {
    fun calculate(
        latitude: Double,
        longitude: Double,
        date: LocalDate = LocalDate.now(),
    ): PrayerDay {
        val coordinates = Coordinates(latitude, longitude)
        val components = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val prayerTimes = PrayerTimes(coordinates, components, CalculationMethod.EGYPTIAN.parameters)
        return PrayerDay(
            fajr = PrayerMoment("الفجر", prayerTimes.fajr.toEpochMilliseconds()),
            sunrise = PrayerMoment("الشروق", prayerTimes.sunrise.toEpochMilliseconds()),
            dhuhr = PrayerMoment("الظهر", prayerTimes.dhuhr.toEpochMilliseconds()),
            asr = PrayerMoment("العصر", prayerTimes.asr.toEpochMilliseconds()),
            maghrib = PrayerMoment("المغرب", prayerTimes.maghrib.toEpochMilliseconds()),
            isha = PrayerMoment("العشاء", prayerTimes.isha.toEpochMilliseconds()),
        )
    }

    fun qiblaDirection(latitude: Double, longitude: Double): Double =
        Qibla(Coordinates(latitude, longitude)).direction

    fun format(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).format(formatter)
    }
}
