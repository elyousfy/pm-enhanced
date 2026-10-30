package com.yahyaoui.prayermode.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.TimeZone

class SilentPlannerTest {
    private val dubai = ZoneId.of("Asia/Dubai")

    private fun day(date: LocalDate, hijriMonth: Int = 5, hijriDay: Int = 10) = DayTimes(
        date = date,
        imsak = LocalTime.of(4, 55),
        fajr = LocalTime.of(5, 5),
        sunrise = LocalTime.of(6, 20),
        dhuhr = LocalTime.of(11, 58),
        asr = LocalTime.of(15, 12),
        maghrib = LocalTime.of(17, 37),
        isha = LocalTime.of(18, 55),
        hijriDay = hijriDay, hijriMonth = hijriMonth, hijriYear = 1448,
    )

    private val thursday = LocalDate.of(2026, 10, 29)
    private val friday = LocalDate.of(2026, 10, 30)
    private val on = Settings(masterOn = true)

    private fun t(date: LocalDate, h: Int, m: Int): Instant = date.atTime(h, m).atZone(dubai).toInstant()

    @Test
    fun masterOffPlansNothing() {
        assertTrue(SilentPlanner.plan(day(thursday), null, Settings(masterOn = false), dubai).isEmpty())
    }

    @Test
    fun defaultPlanHasFiveWindowsOfThirtyMinutes() {
        val w = SilentPlanner.plan(day(thursday), null, on, dubai)
        assertEquals(listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"), w.map { it.key })
        assertEquals(t(thursday, 5, 5), w[0].start)
        assertEquals(t(thursday, 5, 35), w[0].end)
    }

    @Test
    fun disabledPrayerIsSkipped() {
        val s = on.copy(prayers = on.prayers + (Prayer.ASR to PrayerConfig(enabled = false)))
        val keys = SilentPlanner.plan(day(thursday), null, s, dubai).map { it.key }
        assertEquals(listOf("Fajr", "Dhuhr", "Maghrib", "Isha"), keys)
    }

    @Test
    fun inactiveWeekdayPlansNothing() {
        val s = on.copy(activeDays = DayOfWeek.entries.toSet() - DayOfWeek.THURSDAY)
        assertTrue(SilentPlanner.plan(day(thursday), null, s, dubai).isEmpty())
        assertEquals(5, SilentPlanner.plan(day(friday), null, s, dubai).size)
    }

    @Test
    fun fridayUsesFixedUaeJumuaTime() {
        val w = SilentPlanner.plan(day(friday), null, on, dubai)
        val jumua = w.single { it.key == "Jumua" }
        assertEquals(t(friday, 12, 30), jumua.start) // 12:45 - 15
        assertEquals(t(friday, 13, 15), jumua.end)   // 12:45 + 30
        assertTrue(w.none { it.key == "Dhuhr" })
    }

    @Test
    fun jumuaCanFollowDhuhr() {
        val s = on.copy(jumua = JumuaConfig(fixedTime = null, beforeMin = 0, afterMin = 45))
        val jumua = SilentPlanner.plan(day(friday), null, s, dubai).single { it.key == "Jumua" }
        assertEquals(t(friday, 11, 58), jumua.start)
        assertEquals(t(friday, 12, 43), jumua.end)
    }

    @Test
    fun iqamaAndAblutionShiftTheWindow() {
        val s = on.copy(
            takbir = true,
            prayers = on.prayers +
                (Prayer.DHUHR to PrayerConfig(durationMin = 20, iqamaMin = 10)) +
                (Prayer.ASR to PrayerConfig(durationMin = 20, ablution = true)),
        )
        val w = SilentPlanner.plan(day(thursday), null, s, dubai).associateBy { it.key }
        assertEquals(t(thursday, 12, 8), w["Dhuhr"]!!.start)
        assertEquals(t(thursday, 12, 28), w["Dhuhr"]!!.end)
        assertEquals(t(thursday, 11, 58), w["Dhuhr"]!!.takbirAt)
        assertEquals(t(thursday, 15, 2), w["Asr"]!!.start)
        assertEquals(t(thursday, 15, 32), w["Asr"]!!.end)
        assertTrue(w["Asr"]!!.ablution)
        assertNull(w["Asr"]!!.takbirAt)
    }

    @Test
    fun tuneMovesPrayerTime() {
        val s = on.copy(prayers = on.prayers + (Prayer.ISHA to PrayerConfig(tuneMin = -2)))
        val isha = SilentPlanner.plan(day(thursday), null, s, dubai).single { it.key == "Isha" }
        assertEquals(t(thursday, 18, 53), isha.start)
    }

    @Test
    fun resultDoesNotDependOnDeviceTimeZone() {
        val before = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            val w = SilentPlanner.plan(day(thursday), null, on, dubai)
            assertEquals(Instant.parse("2026-10-29T01:05:00Z"), w.first().start) // 05:05 +04
        } finally {
            TimeZone.setDefault(before)
        }
    }

    @Test
    fun londonSummerTimeIsRespected() {
        val m = AladhanApi.parseMonth(javaClass.classLoader!!.getResource("fixtures/aladhan_london_2027_03.json")!!.readText())
        val d = m.days.single { it.date == LocalDate.of(2027, 3, 30) } // after BST starts (28 Mar)
        val w = SilentPlanner.plan(d, null, on, m.zone)
        assertEquals(d.date.atTime(d.fajr).minusHours(1).toInstant(java.time.ZoneOffset.UTC), w.first().start)
    }

    @Test
    fun ramadanNightAddsTaraweehInsteadOfIsha() {
        val lastShaban = day(thursday, hijriMonth = 8, hijriDay = 29)
        val firstRamadan = day(friday, hijriMonth = 9, hijriDay = 1)
        val w = SilentPlanner.plan(lastShaban, firstRamadan, on, dubai)
        assertTrue(w.any { it.key == "Taraweeh" })
        assertTrue(w.none { it.key == "Isha" })
        assertEquals(t(thursday, 19, 55), w.single { it.key == "Taraweeh" }.end) // 60 min default
    }

    @Test
    fun tahajjudExtendsFajrInRamadan() {
        val s = on.copy(ramadan = RamadanConfig(tahajjudBeforeMin = 60))
        val w = SilentPlanner.plan(day(thursday, hijriMonth = 9), null, s, dubai)
        val tahajjud = w.single { it.key == "Tahajjud" }
        assertEquals(t(thursday, 4, 5), tahajjud.start)
        assertEquals(t(thursday, 5, 35), tahajjud.end)
    }

    @Test
    fun eidWindowAfterSunrise() {
        val w = SilentPlanner.plan(day(thursday, hijriMonth = 10, hijriDay = 1), null, on, dubai)
        val eid = w.single { it.key == "Eid" }
        assertEquals(t(thursday, 6, 40), eid.start)
        assertEquals(t(thursday, 7, 10), eid.end)
    }

    @Test
    fun takbirOffMeansNoTakbir() {
        assertTrue(SilentPlanner.plan(day(thursday), null, on, dubai).all { it.takbirAt == null })
    }
}
