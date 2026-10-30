package com.yahyaoui.prayermode.core

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Parses "05:44", "05:44 (+04)" or "05:44 (BST)". */
internal fun parseClock(raw: String): LocalTime {
    val hhmm = raw.trim().substringBefore(' ')
    val (h, m) = hhmm.split(':').map { it.trim().toInt() }
    return LocalTime.of(h, m)
}

object AladhanApi {
    const val BASE = "https://api.aladhan.com/v1"

    /** Aladhan method used when the user keeps "Automatic". */
    fun defaultMethodFor(countryCode: String): Int = when (countryCode.uppercase()) {
        "AE" -> 16 // Dubai; measured within ±1 min of official Awqaf times
        "SA" -> 4
        "KW" -> 9
        "QA" -> 10
        "BH", "OM" -> 8
        "EG", "SY", "LB", "IQ", "SD", "LY" -> 5
        "TR" -> 13
        "SG" -> 11
        "MY" -> 17
        "ID" -> 20
        "FR" -> 12
        "RU" -> 14
        "PK", "IN", "BD", "AF" -> 1
        "US", "CA" -> 2
        "GB", "IE" -> 15
        "TN" -> 18
        "DZ" -> 19
        "MA" -> 21
        "PT" -> 22
        "JO" -> 23
        "IR" -> 7
        else -> 3 // Muslim World League
    }

    fun monthUrl(loc: LocationConfig, ym: YearMonth, method: Int, school: Int): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val common = "method=$method&school=$school"
        return if (loc.latitude != null && loc.longitude != null) {
            val tz = loc.timeZone?.let { "&timezonestring=${enc(it)}" } ?: ""
            "$BASE/calendar/${ym.year}/${ym.monthValue}?latitude=${loc.latitude}&longitude=${loc.longitude}&$common$tz"
        } else {
            "$BASE/calendarByCity/${ym.year}/${ym.monthValue}?city=${enc(loc.city)}&country=${enc(loc.countryCode)}&$common"
        }
    }

    private val gregorian = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun parseMonth(body: String): MonthTimes {
        val root = JSONObject(body)
        if (root.optInt("code") != 200) error("Aladhan error: ${root.opt("data")}")
        val data = root.getJSONArray("data")
        var zone: ZoneId? = null
        val days = (0 until data.length()).map { i ->
            val d = data.getJSONObject(i)
            val t = d.getJSONObject("timings")
            val date = d.getJSONObject("date")
            val hijri = date.getJSONObject("hijri")
            if (zone == null) zone = ZoneId.of(d.getJSONObject("meta").getString("timezone"))
            DayTimes(
                date = LocalDate.parse(date.getJSONObject("gregorian").getString("date"), gregorian),
                imsak = t.optString("Imsak").takeIf { it.isNotBlank() }?.let(::parseClock),
                fajr = parseClock(t.getString("Fajr")),
                sunrise = parseClock(t.getString("Sunrise")),
                dhuhr = parseClock(t.getString("Dhuhr")),
                asr = parseClock(t.getString("Asr")),
                maghrib = parseClock(t.getString("Maghrib")),
                isha = parseClock(t.getString("Isha")),
                hijriDay = hijri.getString("day").toInt(),
                hijriMonth = hijri.getJSONObject("month").getInt("number"),
                hijriYear = hijri.getString("year").toInt(),
            )
        }
        return MonthTimes(zone ?: error("No timezone in Aladhan response"), days)
    }
}

/**
 * UAE General Authority of Islamic Affairs (Awqaf) prayer-time service, as used by awqaf.gov.ae.
 * It needs an API key, which the user supplies; it is never bundled with the app.
 */
object AwqafApi {
    const val BASE = "https://internationalprayertime.awqaf.gov.ae/api/v1"
    val ZONE: ZoneId = ZoneId.of("Asia/Dubai")

    fun monthRequestBody(officialId: Int, ym: YearMonth): String = JSONObject()
        .put("start_date", ym.atDay(1).toString())
        .put("end_date", ym.atEndOfMonth().toString())
        .put("official_id", officialId)
        .toString()

    fun parseMonth(body: String): MonthTimes {
        val root = JSONObject(body)
        val errors = root.optJSONArray("errors") ?: JSONArray()
        if (errors.length() > 0) error("Awqaf error: $errors")
        val list = root.getJSONObject("result").getJSONArray("prayer_times")
        val days = (0 until list.length()).map { i ->
            val d = list.getJSONObject(i)
            val h = d.getJSONObject("hijri_date")
            DayTimes(
                date = LocalDate.parse(d.getString("date")),
                imsak = d.optString("imsak").takeIf { it.contains(':') }?.let(::parseClock),
                fajr = parseClock(d.getString("fajer")),
                sunrise = parseClock(d.getString("shuroq")),
                dhuhr = parseClock(d.getString("dhohur")),
                asr = parseClock(d.getString("aser")),
                maghrib = parseClock(d.getString("maghreb")),
                isha = parseClock(d.getString("isha")),
                hijriDay = h.getInt("day"),
                hijriMonth = h.getInt("month"),
                hijriYear = h.getInt("year"),
            )
        }
        return MonthTimes(ZONE, days)
    }
}

/** Our own cache format, shared by both sources. */
object MonthCodec {
    fun encode(m: MonthTimes): String {
        val arr = JSONArray()
        m.days.forEach { d ->
            arr.put(
                JSONObject()
                    .put("date", d.date.toString())
                    .put("imsak", d.imsak?.toString() ?: "")
                    .put("fajr", d.fajr.toString())
                    .put("sunrise", d.sunrise.toString())
                    .put("dhuhr", d.dhuhr.toString())
                    .put("asr", d.asr.toString())
                    .put("maghrib", d.maghrib.toString())
                    .put("isha", d.isha.toString())
                    .put("hd", d.hijriDay).put("hm", d.hijriMonth).put("hy", d.hijriYear)
            )
        }
        return JSONObject().put("zone", m.zone.id).put("days", arr).toString()
    }

    fun decode(s: String): MonthTimes {
        val o = JSONObject(s)
        val arr = o.getJSONArray("days")
        val days = (0 until arr.length()).map { i ->
            val d = arr.getJSONObject(i)
            DayTimes(
                date = LocalDate.parse(d.getString("date")),
                imsak = d.getString("imsak").takeIf { it.isNotEmpty() }?.let(LocalTime::parse),
                fajr = LocalTime.parse(d.getString("fajr")),
                sunrise = LocalTime.parse(d.getString("sunrise")),
                dhuhr = LocalTime.parse(d.getString("dhuhr")),
                asr = LocalTime.parse(d.getString("asr")),
                maghrib = LocalTime.parse(d.getString("maghrib")),
                isha = LocalTime.parse(d.getString("isha")),
                hijriDay = d.getInt("hd"), hijriMonth = d.getInt("hm"), hijriYear = d.getInt("hy"),
            )
        }
        return MonthTimes(ZoneId.of(o.getString("zone")), days)
    }
}
