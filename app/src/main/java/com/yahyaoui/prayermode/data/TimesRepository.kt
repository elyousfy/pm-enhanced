package com.yahyaoui.prayermode.data

import android.content.Context
import android.util.Log
import com.yahyaoui.prayermode.BuildConfig
import com.yahyaoui.prayermode.core.AladhanApi
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.AwqafApi
import com.yahyaoui.prayermode.core.DayTimes
import com.yahyaoui.prayermode.core.MonthCodec
import com.yahyaoui.prayermode.core.MonthTimes
import com.yahyaoui.prayermode.core.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

data class MonthResult(val month: MonthTimes, val source: TimeSource, val fellBack: Boolean)

/**
 * Month-by-month prayer times with an on-disk cache.
 * A month is only written to the cache after a successful download, so a failed fetch
 * never hides behind stale data.
 */
class TimesRepository(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "times").apply { mkdirs() }
    private val tag = "TimesRepository"

    private fun key(c: AppConfig, source: TimeSource): String {
        val l = c.location
        val raw = when (source) {
            TimeSource.AWQAF -> "awqaf|${l.awqafLocation}"
            TimeSource.ALADHAN -> "aladhan|${c.effectiveMethod}|${c.asrSchool}|${l.countryCode}|${l.city}|${l.latitude}|${l.longitude}|${l.timeZone}"
        }
        return Integer.toHexString(raw.hashCode())
    }

    private fun file(c: AppConfig, source: TimeSource, ym: YearMonth) = File(dir, "${source.id}-${key(c, source)}-$ym.json")

    fun cached(c: AppConfig, ym: YearMonth): MonthResult? {
        for (source in sourcesFor(c)) {
            val f = file(c, source, ym)
            if (f.exists()) {
                runCatching { MonthCodec.decode(f.readText()) }.getOrNull()?.let {
                    return MonthResult(it, source, source != c.source)
                }
            }
        }
        return null
    }

    /** Cached month, else downloads it (Awqaf first if chosen, Aladhan as fallback). */
    suspend fun month(c: AppConfig, ym: YearMonth, forceRefresh: Boolean = false): MonthResult = withContext(Dispatchers.IO) {
        require(c.location.city.isNotBlank() || c.location.latitude != null) { "No city set" }
        if (!forceRefresh) cached(c, ym)?.takeIf { it.source == c.source || !canUseAwqaf(c) }?.let { return@withContext it }
        var lastError: Exception? = null
        for (source in sourcesFor(c)) {
            try {
                val month = download(c, source, ym)
                file(c, source, ym).writeText(MonthCodec.encode(month))
                SettingsStore.putString(appContext, LAST_ERROR, null)
                return@withContext MonthResult(month, source, source != c.source)
            } catch (e: Exception) {
                Log.w(tag, "Fetch failed from ${source.id} for $ym: ${e.message}")
                lastError = e
            }
        }
        cached(c, ym)?.let { return@withContext it }
        SettingsStore.putString(appContext, LAST_ERROR, lastError?.message ?: "unknown")
        throw IOException("Could not load prayer times for $ym", lastError)
    }

    suspend fun day(c: AppConfig, date: LocalDate): Pair<DayTimes, MonthResult>? {
        val m = month(c, YearMonth.from(date))
        return m.month.days.firstOrNull { it.date == date }?.let { it to m }
    }

    private val appContext = context.applicationContext

    private fun canUseAwqaf(c: AppConfig) =
        c.source == TimeSource.AWQAF && c.location.awqafLocation != null && c.awqafApiKey.isNotBlank()

    private fun sourcesFor(c: AppConfig): List<TimeSource> =
        if (canUseAwqaf(c)) listOf(TimeSource.AWQAF, TimeSource.ALADHAN) else listOf(TimeSource.ALADHAN)

    private fun download(c: AppConfig, source: TimeSource, ym: YearMonth): MonthTimes {
        val request = when (source) {
            TimeSource.ALADHAN -> Request.Builder().url(AladhanApi.monthUrl(c.location, ym, c.effectiveMethod, c.asrSchool)).build()
            TimeSource.AWQAF -> Request.Builder()
                .url("${AwqafApi.BASE}/prayertimes")
                .header("Accept", "application/json")
                .header("x-api-key", c.awqafApiKey)
                .post(AwqafApi.monthRequestBody(c.location.awqafLocation!!, ym).toRequestBody("application/json".toMediaType()))
                .build()
        }
        if (BuildConfig.DEBUG) Log.i(tag, "GET ${request.url}")
        client.newCall(request).execute().use { res ->
            if (!res.isSuccessful) throw IOException("HTTP ${res.code} from ${source.id}")
            val body = res.body.string()
            val month = if (source == TimeSource.ALADHAN) AladhanApi.parseMonth(body) else AwqafApi.parseMonth(body)
            if (month.days.isEmpty()) throw IOException("Empty month from ${source.id}")
            return month
        }
    }

    companion object {
        const val LAST_ERROR = "last_fetch_error"
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
