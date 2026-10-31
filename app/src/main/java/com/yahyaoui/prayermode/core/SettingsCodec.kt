package com.yahyaoui.prayermode.core

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime

/** Everything the user configures, stored as one JSON document. */
data class AppConfig(
    val settings: Settings = Settings(),
    val location: LocationConfig = UaeCities.default,
    val source: TimeSource = TimeSource.ALADHAN,
    /** Aladhan method id; null = automatic by country. */
    val method: Int? = null,
    /** 0 = Shafi/standard, 1 = Hanafi. */
    val asrSchool: Int = 0,
    val awqafApiKey: String = "",
    val notifySilence: Boolean = true,
    /** Notification at each prayer time, independent of silencing. */
    val notifyPrayer: Boolean = false,
    val weekStartsSunday: Boolean = false,
    val onboarded: Boolean = false,
    /** "system", "light", "midnight" (dark) or "forest". */
    val theme: String = "system",
    val accent: String = "green",
    /** "system", "24" or "12". */
    val clock: String = "system",
) {
    val effectiveMethod: Int get() = method ?: AladhanApi.defaultMethodFor(location.countryCode)
}

object SettingsCodec {
    fun encode(c: AppConfig): String {
        val s = c.settings
        val prayers = JSONObject()
        s.prayers.forEach { (p, pc) ->
            prayers.put(
                p.key, JSONObject()
                    .put("enabled", pc.enabled).put("duration", pc.durationMin)
                    .put("ablution", pc.ablution).put("iqama", pc.iqamaMin).put("tune", pc.tuneMin)
            )
        }
        val loc = c.location
        return JSONObject()
            .put("v", 1)
            .put("masterOn", s.masterOn)
            .put("takbir", s.takbir)
            .put("prayers", prayers)
            .put("activeDays", JSONArray(s.activeDays.map { it.name }))
            .put(
                "jumua", JSONObject().put("enabled", s.jumua.enabled)
                    .put("fixed", s.jumua.fixedTime?.toString() ?: "")
                    .put("before", s.jumua.beforeMin).put("after", s.jumua.afterMin)
            )
            .put(
                "ramadan", JSONObject().put("taraweeh", s.ramadan.taraweehEnabled)
                    .put("taraweehDuration", s.ramadan.taraweehDurationMin)
                    .put("taraweehAblution", s.ramadan.taraweehAblution)
                    .put("taraweehIqama", s.ramadan.taraweehIqamaMin)
                    .put("tahajjud", s.ramadan.tahajjudBeforeMin)
            )
            .put("eid", JSONObject().put("fixed", s.eid.fixedTime?.toString() ?: "").put("duration", s.eid.durationMin))
            .put(
                "location", JSONObject().put("country", loc.countryCode).put("city", loc.city)
                    .put("lat", loc.latitude ?: JSONObject.NULL).put("lng", loc.longitude ?: JSONObject.NULL)
                    .put("tz", loc.timeZone ?: JSONObject.NULL).put("awqaf", loc.awqafLocation ?: JSONObject.NULL)
            )
            .put("source", c.source.id)
            .put("method", c.method ?: JSONObject.NULL)
            .put("school", c.asrSchool)
            .put("awqafKey", c.awqafApiKey)
            .put("notifySilence", c.notifySilence)
            .put("notifyPrayer", c.notifyPrayer)
            .put("weekStartsSunday", c.weekStartsSunday)
            .put("onboarded", c.onboarded)
            .put("theme", c.theme)
            .put("accent", c.accent)
            .put("clock", c.clock)
            .toString()
    }

    fun decode(json: String?): AppConfig {
        if (json.isNullOrBlank()) return AppConfig()
        val o = JSONObject(json)
        val d = Settings()
        val prayersJson = o.optJSONObject("prayers") ?: JSONObject()
        val prayers = Prayer.entries.associateWith { p ->
            val j = prayersJson.optJSONObject(p.key) ?: return@associateWith PrayerConfig()
            PrayerConfig(
                enabled = j.optBoolean("enabled", true),
                durationMin = j.optInt("duration", 30),
                ablution = j.optBoolean("ablution", false),
                iqamaMin = j.optInt("iqama", 0),
                tuneMin = j.optInt("tune", 0),
            )
        }
        val days = o.optJSONArray("activeDays")?.let { a ->
            (0 until a.length()).mapNotNull { runCatching { DayOfWeek.valueOf(a.getString(it)) }.getOrNull() }.toSet()
        } ?: d.activeDays
        fun time(s: String?) = s?.takeIf { it.isNotBlank() }?.let(LocalTime::parse)
        val j = o.optJSONObject("jumua")
        val r = o.optJSONObject("ramadan")
        val e = o.optJSONObject("eid")
        val l = o.optJSONObject("location")
        fun JSONObject.optDoubleOrNull(k: String) = if (isNull(k) || !has(k)) null else getDouble(k)
        fun JSONObject.optIntOrNull(k: String) = if (isNull(k) || !has(k)) null else getInt(k)
        fun JSONObject.optStringOrNull(k: String) = if (isNull(k) || !has(k)) null else getString(k)
        return AppConfig(
            settings = Settings(
                masterOn = o.optBoolean("masterOn", false),
                prayers = prayers,
                activeDays = days,
                jumua = if (j == null) d.jumua else JumuaConfig(
                    enabled = j.optBoolean("enabled", true),
                    fixedTime = time(j.optString("fixed")),
                    beforeMin = j.optInt("before", d.jumua.beforeMin),
                    afterMin = j.optInt("after", d.jumua.afterMin),
                ),
                ramadan = if (r == null) d.ramadan else RamadanConfig(
                    taraweehEnabled = r.optBoolean("taraweeh", true),
                    taraweehDurationMin = r.optInt("taraweehDuration", 60),
                    taraweehAblution = r.optBoolean("taraweehAblution", false),
                    taraweehIqamaMin = r.optInt("taraweehIqama", 0),
                    tahajjudBeforeMin = r.optInt("tahajjud", 0),
                ),
                eid = if (e == null) d.eid else EidConfig(time(e.optString("fixed")), e.optInt("duration", 30)),
                takbir = o.optBoolean("takbir", false),
            ),
            location = if (l == null) UaeCities.default else LocationConfig(
                countryCode = l.optString("country", "AE"),
                city = l.optString("city", "Abu Dhabi"),
                latitude = l.optDoubleOrNull("lat"),
                longitude = l.optDoubleOrNull("lng"),
                timeZone = l.optStringOrNull("tz"),
                awqafLocation = l.optIntOrNull("awqaf"),
            ),
            source = TimeSource.from(o.optString("source")),
            method = o.optIntOrNull("method"),
            asrSchool = o.optInt("school", 0),
            awqafApiKey = o.optString("awqafKey", ""),
            notifySilence = o.optBoolean("notifySilence", true),
            notifyPrayer = o.optBoolean("notifyPrayer", false),
            weekStartsSunday = o.optBoolean("weekStartsSunday", false),
            onboarded = o.optBoolean("onboarded", false),
            theme = o.optString("theme", "system"),
            accent = o.optString("accent", "green"),
            clock = o.optString("clock", "system"),
        )
    }
}
