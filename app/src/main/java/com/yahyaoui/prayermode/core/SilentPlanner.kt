package com.yahyaoui.prayermode.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * One period of Do Not Disturb.
 * [key] is the prayer name used in notifications ("Fajr", "Jumua", "Taraweeh", ...).
 * [adhan] is the prayer time itself. [takbirAt] is when to play the takbir, or null for none.
 */
data class SilentWindow(
    val key: String,
    val adhan: Instant,
    val start: Instant,
    val end: Instant,
    val ablution: Boolean,
    val takbirAt: Instant?,
)

/**
 * Pure planning: from one day's times and the user's settings, returns the silent windows
 * for that day, sorted by start. Does not filter out windows already in the past.
 *
 * [nextDay] is used to tell whether tonight is a Ramadan night (Taraweeh).
 */
object SilentPlanner {
    const val ABLUTION_MIN = 10L
    const val EID_AFTER_SUNRISE_MIN = 20L

    fun plan(rawDay: DayTimes, nextDay: DayTimes?, s: Settings, zone: ZoneId): List<SilentWindow> {
        if (!s.masterOn) return emptyList()
        val day = rawDay.withTune(s.tune)
        val weekday = day.date.dayOfWeek
        if (weekday !in s.activeDays) return emptyList()

        fun at(t: LocalTime): Instant = day.date.atTime(t).atZone(zone).toInstant()
        fun min(m: Number) = m.toLong() * 60_000L

        val isRamadan = day.hijriMonth == 9
        // Taraweeh is prayed on the nights before Ramadan days.
        val taraweehTonight = if (nextDay != null) nextDay.hijriMonth == 9 else isRamadan && day.hijriDay < 29
        val isEid = (day.hijriMonth == 10 && day.hijriDay == 1) || (day.hijriMonth == 12 && day.hijriDay == 10)

        val out = mutableListOf<SilentWindow>()

        fun regular(key: String, adhan: Instant, duration: Int, ablution: Boolean, iqama: Int, extraBeforeMin: Int = 0) {
            val iqamaOn = iqama > 0
            val ablutionOn = ablution && !iqamaOn
            val start = when {
                extraBeforeMin > 0 -> adhan.minusMillis(min(extraBeforeMin))
                iqamaOn -> adhan.plusMillis(min(iqama))
                ablutionOn -> adhan.minusMillis(min(ABLUTION_MIN))
                else -> adhan
            }
            val end = if (iqamaOn && extraBeforeMin == 0) start.plusMillis(min(duration)) else adhan.plusMillis(min(duration))
            val takbir = if (!s.takbir || ablutionOn || extraBeforeMin > 0) null else adhan
            out += SilentWindow(key, adhan, start, end, ablutionOn, takbir)
        }

        for (p in Prayer.entries) {
            val c = s.prayer(p)
            if (!c.enabled) continue
            val adhan = at(day.time(p))
            when {
                p == Prayer.DHUHR && weekday == DayOfWeek.FRIDAY -> {
                    if (s.jumua.enabled) {
                        val anchor = at(s.jumua.fixedTime ?: day.dhuhr)
                        out += SilentWindow(
                            "Jumua", anchor,
                            anchor.minusMillis(min(s.jumua.beforeMin)),
                            anchor.plusMillis(min(s.jumua.afterMin)),
                            ablution = false, takbirAt = null,
                        )
                    }
                }
                p == Prayer.ISHA && taraweehTonight && s.ramadan.taraweehEnabled -> {
                    val r = s.ramadan
                    regular("Taraweeh", adhan, r.taraweehDurationMin, r.taraweehAblution, r.taraweehIqamaMin)
                }
                p == Prayer.FAJR && isRamadan && s.ramadan.tahajjudBeforeMin > 0 -> {
                    regular("Tahajjud", adhan, c.durationMin, false, 0, extraBeforeMin = s.ramadan.tahajjudBeforeMin)
                }
                else -> regular(p.key, adhan, c.durationMin, c.ablution, c.iqamaMin)
            }
        }

        if (isEid && s.eid.durationMin > 0) {
            val start = s.eid.fixedTime?.let { at(it) } ?: at(day.sunrise).plusMillis(min(EID_AFTER_SUNRISE_MIN))
            out += SilentWindow("Eid", start, start, start.plusMillis(min(s.eid.durationMin)), false, null)
        }

        return out.sortedBy { it.start }
    }
}
