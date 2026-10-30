package com.yahyaoui.prayermode.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

class TimesApiTest {
    private fun fixture(name: String) =
        javaClass.classLoader!!.getResource("fixtures/$name")!!.readText()

    @Test
    fun parsesAladhanMonth() {
        val m = AladhanApi.parseMonth(fixture("aladhan_abudhabi_2027_01.json"))
        assertEquals(ZoneId.of("Asia/Dubai"), m.zone)
        assertEquals(31, m.days.size)
        val d = m.days.first()
        assertEquals(LocalDate.of(2027, 1, 1), d.date)
        assertEquals(LocalTime.of(5, 44), d.fajr)
        assertEquals(LocalTime.of(19, 7), d.isha)
        assertEquals(23, d.hijriDay); assertEquals(7, d.hijriMonth); assertEquals(1448, d.hijriYear)
    }

    @Test
    fun parsesAwqafMonth() {
        val m = AwqafApi.parseMonth(fixture("awqaf_abudhabi_2027_01.json"))
        assertEquals(31, m.days.size)
        val d = m.days.first()
        assertEquals(LocalTime.of(5, 44), d.fajr)
        assertEquals(LocalTime.of(7, 3), d.sunrise)
        assertEquals(LocalTime.of(12, 28), d.dhuhr)
        assertEquals(LocalTime.of(19, 7), d.isha)
        assertEquals(LocalTime.of(5, 34), d.imsak)
        assertEquals(7, d.hijriMonth)
    }

    @Test
    fun parsesClockSuffixes() {
        assertEquals(LocalTime.of(5, 6), parseClock("05:06 (BST)"))
        assertEquals(LocalTime.of(5, 44), parseClock("05:44 (+04)"))
        assertEquals(LocalTime.of(23, 59), parseClock("23:59"))
    }

    /** Default UAE method must stay within one minute of official Awqaf times (Abu Dhabi 2027). */
    @Test
    fun aladhanDubaiMethodMatchesOfficialAwqafWithinOneMinute() {
        val official = fixture("awqaf_abu_dhabi_2027.csv").lines().drop(1).filter { it.isNotBlank() }
            .associate { line ->
                val c = line.split(',')
                LocalDate.parse(c[0]) to c.drop(1).map { LocalTime.parse(it) }
            }
        var compared = 0
        var worst = 0
        for (month in listOf("01", "04", "07", "10")) {
            AladhanApi.parseMonth(fixture("aladhan_abudhabi_2027_$month.json")).days.forEach { d ->
                val ref = official[d.date] ?: return@forEach
                // ref columns: fajr, sunrise, dhuhr, asr, maghrib, isha
                val ours = listOf(d.fajr, d.dhuhr, d.asr, d.maghrib, d.isha)
                val theirs = listOf(ref[0], ref[2], ref[3], ref[4], ref[5])
                ours.zip(theirs).forEach { (a, b) ->
                    val delta = abs(a.toSecondOfDay() - b.toSecondOfDay()) / 60
                    worst = maxOf(worst, delta)
                }
                compared++
            }
        }
        assertTrue("compared $compared days", compared >= 120)
        assertTrue("worst delta $worst min", worst <= 1)
    }

    @Test
    fun uaeDefaultsToDubaiMethod() {
        assertEquals(16, AladhanApi.defaultMethodFor("AE"))
        assertEquals(16, AladhanApi.defaultMethodFor("ae"))
    }

    @Test
    fun buildsUrls() {
        val byCoords = AladhanApi.monthUrl(UaeCities.default, YearMonth.of(2027, 1), 16, 0)
        assertEquals(
            "https://api.aladhan.com/v1/calendar/2027/1?latitude=24.4539&longitude=54.3773&method=16&school=0&timezonestring=Asia%2FDubai",
            byCoords,
        )
        val byCity = AladhanApi.monthUrl(LocationConfig("GB", "London"), YearMonth.of(2027, 3), 15, 1)
        assertEquals("https://api.aladhan.com/v1/calendarByCity/2027/3?city=London&country=GB&method=15&school=1", byCity)
        val body = org.json.JSONObject(AwqafApi.monthRequestBody(36, YearMonth.of(2027, 2)))
        assertEquals("2027-02-01", body.getString("start_date"))
        assertEquals("2027-02-28", body.getString("end_date"))
        assertEquals(36, body.getInt("official_id"))
    }

    @Test
    fun cacheCodecRoundTrips() {
        val m = AladhanApi.parseMonth(fixture("aladhan_london_2027_03.json"))
        assertEquals(m, MonthCodec.decode(MonthCodec.encode(m)))
    }
}

class SettingsCodecTest {
    @Test
    fun roundTripsEveryField() {
        val c = AppConfig(
            settings = Settings(
                masterOn = true,
                prayers = Prayer.entries.associateWith { PrayerConfig(enabled = it != Prayer.ASR, durationMin = 25, ablution = it == Prayer.FAJR, iqamaMin = if (it == Prayer.DHUHR) 10 else 0, tuneMin = -2) },
                activeDays = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.FRIDAY),
                jumua = JumuaConfig(true, LocalTime.of(13, 0), 10, 40),
                ramadan = RamadanConfig(false, 90, true, 5, 45),
                eid = EidConfig(LocalTime.of(6, 30), 45),
                takbir = true,
            ),
            location = LocationConfig("GB", "London", null, null, "Europe/London", null),
            source = TimeSource.AWQAF, method = 15, asrSchool = 1, awqafApiKey = "k",
            notifySilence = false, weekStartsSunday = true, onboarded = true,
            theme = "light", accent = "mono", clock = "12",
        )
        assertEquals(c, SettingsCodec.decode(SettingsCodec.encode(c)))
        assertEquals(AppConfig(), SettingsCodec.decode(null))
    }
}
