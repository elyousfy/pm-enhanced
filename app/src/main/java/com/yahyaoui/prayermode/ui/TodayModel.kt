package com.yahyaoui.prayermode.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yahyaoui.prayermode.PrayerScheduler
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.DayTimes
import com.yahyaoui.prayermode.core.Prayer
import com.yahyaoui.prayermode.core.SilentPlanner
import com.yahyaoui.prayermode.core.SilentWindow
import com.yahyaoui.prayermode.core.TimeSource
import com.yahyaoui.prayermode.data.TimesRepository
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayData(
    val day: DayTimes,
    val tomorrow: DayTimes?,
    val zone: ZoneId,
    val source: TimeSource,
    val fellBack: Boolean,
)

sealed interface TodayState {
    data object Loading : TodayState
    data class Failed(val message: String?) : TodayState
    data class Ready(val data: TodayData) : TodayState
}

/** Loads today + tomorrow for [config]; reloads when the config's time inputs or [reload] change. */
@Composable
fun rememberToday(config: AppConfig, reload: Int): State<TodayState> {
    val ctx = LocalContext.current.applicationContext
    val key = listOf(config.location, config.source, config.effectiveMethod, config.asrSchool, config.awqafApiKey, reload)
    return produceState<TodayState>(TodayState.Loading, key) {
        value = TodayState.Loading
        value = loadToday(ctx, config, force = reload > 0)
    }
}

suspend fun loadToday(ctx: Context, config: AppConfig, force: Boolean = false): TodayState {
    val repo = TimesRepository(ctx)
    val zone = PrayerScheduler.zoneOf(config)
    val today = LocalDate.now(zone)
    return try {
        val m = repo.month(config, java.time.YearMonth.from(today), forceRefresh = force)
        val day = m.month.days.firstOrNull { it.date == today } ?: return TodayState.Failed(null)
        val tomorrowDate = today.plusDays(1)
        val tomorrow = m.month.days.firstOrNull { it.date == tomorrowDate }
            ?: runCatching { repo.day(config, tomorrowDate)?.first }.getOrNull()
        TodayState.Ready(TodayData(day, tomorrow, m.month.zone, m.source, m.fellBack))
    } catch (e: Exception) {
        TodayState.Failed(e.message)
    }
}

/** Wall-clock ticker aligned to the second. */
@Composable
fun rememberNow(): State<Instant> {
    val now = remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.value = Instant.now()
            delay(1000 - (System.currentTimeMillis() % 1000))
        }
    }
    return now
}

/** One row of today's list; [at] is the shown time (tuned adhan, or the fixed Jumuʿah time). */
data class PrayerSlot(val prayer: Prayer, val key: String, val at: Instant, val window: SilentWindow?)

data class DayPlan(val slots: List<PrayerSlot>, val windows: List<SilentWindow>)

/** Plans [day] as if Prayer Mode were on, so the UI can preview windows while it is off. */
fun dayPlan(day: DayTimes, next: DayTimes?, config: AppConfig, zone: ZoneId): DayPlan {
    val s = config.settings
    val windows = SilentPlanner.plan(day, next, s.copy(masterOn = true), zone)
    val tuned = day.withTune(s.tune)
    val slots = Prayer.entries.map { p ->
        val keys = when (p) {
            Prayer.FAJR -> setOf("Fajr", "Tahajjud")
            Prayer.DHUHR -> setOf("Dhuhr", "Jumua")
            Prayer.ISHA -> setOf("Isha", "Taraweeh")
            else -> setOf(p.key)
        }
        val w = windows.firstOrNull { it.key in keys }
        val jumua = p == Prayer.DHUHR && day.date.dayOfWeek == DayOfWeek.FRIDAY && s.jumua.enabled
        val time = if (jumua) s.jumua.fixedTime ?: tuned.dhuhr else tuned.time(p)
        PrayerSlot(p, w?.key ?: if (jumua) "Jumua" else p.key, day.date.atTime(time).atZone(zone).toInstant(), w)
    }
    return DayPlan(slots, windows)
}

/** What the hero shows. */
data class HeroInfo(
    val key: String,
    val adhan: Instant,
    val window: SilentWindow?,
    val silentNow: Boolean,
    val tomorrow: Boolean,
    val progress: Float,
    val target: Instant,
)

fun heroInfo(data: TodayData, config: AppConfig, now: Instant): HeroInfo {
    val today = dayPlan(data.day, data.tomorrow, config, data.zone)
    val silent = if (config.settings.masterOn) today.windows.firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) } else null
    if (silent != null) {
        val total = (silent.end.toEpochMilli() - silent.start.toEpochMilli()).coerceAtLeast(1)
        val p = ((now.toEpochMilli() - silent.start.toEpochMilli()).toFloat() / total).coerceIn(0f, 1f)
        return HeroInfo(silent.key, silent.adhan, silent, true, false, p, silent.end)
    }
    val nextToday = today.slots.firstOrNull { it.at.isAfter(now) }
    val tomorrowSlot = data.tomorrow?.let { dayPlan(it, null, config, data.zone).slots.first() }
    val next = nextToday ?: tomorrowSlot
        ?: today.slots.first().let { it.copy(at = it.at.plusSeconds(86_400)) }
    val previous = today.slots.lastOrNull { !it.at.isAfter(now) }?.at ?: today.slots.last().at.minusSeconds(86_400)
    val total = (next.at.toEpochMilli() - previous.toEpochMilli()).coerceAtLeast(1)
    val progress = ((now.toEpochMilli() - previous.toEpochMilli()).toFloat() / total).coerceIn(0f, 1f)
    return HeroInfo(next.key, next.at, next.window, false, nextToday == null, progress, next.at)
}

/** Bumped from the UI to force a network refresh. */
class Reloader { val count = mutableIntStateOf(0) }
