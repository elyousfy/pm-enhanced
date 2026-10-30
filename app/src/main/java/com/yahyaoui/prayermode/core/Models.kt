package com.yahyaoui.prayermode.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class Prayer(val key: String) {
    FAJR("Fajr"), DHUHR("Dhuhr"), ASR("Asr"), MAGHRIB("Maghrib"), ISHA("Isha");
}

/** Prayer times for one day, in the location's own time zone. */
data class DayTimes(
    val date: LocalDate,
    val imsak: LocalTime?,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
) {
    fun time(p: Prayer): LocalTime = when (p) {
        Prayer.FAJR -> fajr
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    /** Applies the user's per-prayer minute offsets. */
    fun withTune(tune: Map<Prayer, Int>): DayTimes = copy(
        fajr = fajr.plusMinutes((tune[Prayer.FAJR] ?: 0).toLong()),
        dhuhr = dhuhr.plusMinutes((tune[Prayer.DHUHR] ?: 0).toLong()),
        asr = asr.plusMinutes((tune[Prayer.ASR] ?: 0).toLong()),
        maghrib = maghrib.plusMinutes((tune[Prayer.MAGHRIB] ?: 0).toLong()),
        isha = isha.plusMinutes((tune[Prayer.ISHA] ?: 0).toLong()),
    )
}

data class MonthTimes(val zone: ZoneId, val days: List<DayTimes>)

enum class TimeSource(val id: String) {
    ALADHAN("aladhan"), AWQAF("awqaf");

    companion object {
        fun from(id: String?) = entries.firstOrNull { it.id == id } ?: ALADHAN
    }
}

/**
 * Where the user is. [latitude]/[longitude] are set for preset cities; free-text cities
 * are geocoded by Aladhan. [awqafLocation] is the Awqaf official_id (UAE presets only).
 */
data class LocationConfig(
    val countryCode: String,
    val city: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timeZone: String? = null,
    val awqafLocation: Int? = null,
)

data class PrayerConfig(
    val enabled: Boolean = true,
    val durationMin: Int = 30,
    val ablution: Boolean = false,
    val iqamaMin: Int = 0,
    val tuneMin: Int = 0,
)

data class JumuaConfig(
    val enabled: Boolean = true,
    /** Fixed iqama time (UAE: 12:45). Null = anchor on that day's Dhuhr. */
    val fixedTime: LocalTime? = LocalTime.of(12, 45),
    val beforeMin: Int = 15,
    val afterMin: Int = 30,
)

data class RamadanConfig(
    val taraweehEnabled: Boolean = true,
    val taraweehDurationMin: Int = 60,
    val taraweehAblution: Boolean = false,
    val taraweehIqamaMin: Int = 0,
    /** Minutes of silence before Fajr during Ramadan; 0 = off. */
    val tahajjudBeforeMin: Int = 0,
)

data class EidConfig(
    /** Null = 20 minutes after sunrise. */
    val fixedTime: LocalTime? = null,
    /** 0 = off. */
    val durationMin: Int = 30,
)

data class Settings(
    val masterOn: Boolean = false,
    val prayers: Map<Prayer, PrayerConfig> = Prayer.entries.associateWith { PrayerConfig() },
    val activeDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val jumua: JumuaConfig = JumuaConfig(),
    val ramadan: RamadanConfig = RamadanConfig(),
    val eid: EidConfig = EidConfig(),
    val takbir: Boolean = false,
) {
    fun prayer(p: Prayer) = prayers[p] ?: PrayerConfig()
    val tune: Map<Prayer, Int> get() = prayers.mapValues { it.value.tuneMin }
}
