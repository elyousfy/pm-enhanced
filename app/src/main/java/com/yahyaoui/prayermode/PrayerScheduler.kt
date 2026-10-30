package com.yahyaoui.prayermode

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.DayTimes
import com.yahyaoui.prayermode.core.SilentPlanner
import com.yahyaoui.prayermode.core.SilentWindow
import com.yahyaoui.prayermode.data.SettingsStore
import com.yahyaoui.prayermode.data.TimesRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Turns today's plan into exact alarms. Rebuilt on every settings change, at 00:01 in the
 * location's time zone, after boot, and when the device clock or time zone changes.
 */
object PrayerScheduler {
    private const val TAG = "PrayerScheduler"
    private const val MAX_SLOTS = 12
    private const val DAILY_REQUEST = 3123
    const val BACKUP_END_MIN = 5L

    const val ACTION_START = "com.yahyaoui.prayermode.START_SILENT"
    const val ACTION_END = "com.yahyaoui.prayermode.END_SILENT"
    const val ACTION_BACKUP_END = "com.yahyaoui.prayermode.BACKUP_END_SILENT"
    const val ACTION_TAKBIR = "com.yahyaoui.prayermode.TAKBIR"
    const val ACTION_DAILY = "com.yahyaoui.prayermode.DAILY"

    const val EXTRA_KEY = "key"
    const val EXTRA_ABLUTION = "ablution"
    const val EXTRA_TAKBIR = "takbir"

    sealed interface Outcome {
        data class Scheduled(val windows: List<SilentWindow>, val exact: Boolean) : Outcome
        data object Off : Outcome
        data class NoData(val error: String?) : Outcome
    }

    fun zoneOf(c: AppConfig): ZoneId = c.location.timeZone?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()

    suspend fun rebuild(context: Context): Outcome {
        val ctx = context.applicationContext
        val config = SettingsStore.current(ctx)
        cancelAll(ctx)
        if (!config.settings.masterOn) {
            cancelDaily(ctx)
            releaseIfUnplanned(ctx, emptyList())
            return Outcome.Off
        }
        val repo = TimesRepository(ctx)
        val zone = zoneOf(config)
        val today = LocalDate.now(zone)
        val todayResult = try {
            repo.day(config, today)
        } catch (e: Exception) {
            Log.e(TAG, "No prayer times for $today", e)
            null
        }
        if (todayResult == null) {
            releaseIfUnplanned(ctx, emptyList())
            scheduleDaily(ctx, zone)
            return Outcome.NoData(SettingsStore.getString(ctx, TimesRepository.LAST_ERROR))
        }
        val (day, monthResult) = todayResult
        val nextDay = nextDay(repo, config, day)
        val windows = SilentPlanner.plan(day, nextDay, config.settings, monthResult.month.zone)
        val exact = schedule(ctx, windows)
        releaseIfUnplanned(ctx, windows)
        scheduleDaily(ctx, monthResult.month.zone)
        if (today.dayOfMonth >= 20) {
            runCatching { repo.month(config, YearMonth.from(today).plusMonths(1)) }
        }
        return Outcome.Scheduled(windows, exact)
    }

    /** cancelAll() dropped any pending END alarm, so give sound back if no window covers now. */
    private fun releaseIfUnplanned(ctx: Context, windows: List<SilentWindow>) {
        val now = Instant.now()
        if (DndController.isAppControlledActive(ctx) && windows.none { !now.isBefore(it.start) && now.isBefore(it.end) }) {
            DndController.disable(ctx, notify = false)
        }
    }

    private suspend fun nextDay(repo: TimesRepository, c: AppConfig, day: DayTimes): DayTimes? {
        val next = day.date.plusDays(1)
        return repo.cached(c, YearMonth.from(next))?.month?.days?.firstOrNull { it.date == next }
            ?: runCatching { repo.day(c, next)?.first }.getOrNull()
    }

    /** True when [key]'s window still exists in today's plan and covers [now] (re-checked when an alarm fires). */
    fun isPlannedNow(context: Context, key: String, now: Instant = Instant.now(), slackMs: Long = 120_000): Boolean {
        val c = SettingsStore.current(context)
        if (!c.settings.masterOn) return false
        val repo = TimesRepository(context)
        val zone = zoneOf(c)
        val today = LocalDate.now(zone)
        val month = repo.cached(c, YearMonth.from(today))?.month ?: return true // no cache: trust the alarm
        val day = month.days.firstOrNull { it.date == today } ?: return true
        val next = repo.cached(c, YearMonth.from(today.plusDays(1)))?.month?.days?.firstOrNull { it.date == today.plusDays(1) }
        return SilentPlanner.plan(day, next, c.settings, month.zone).any {
            it.key == key && now.toEpochMilli() in (it.start.toEpochMilli() - slackMs) until it.end.toEpochMilli()
        }
    }

    private fun schedule(ctx: Context, windows: List<SilentWindow>): Boolean {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        val now = Instant.now()
        windows.filter { it.end.isAfter(now) }.take(MAX_SLOTS).forEachIndexed { slot, w ->
            if (!now.isBefore(w.start)) {
                // Already inside the window (e.g. settings changed mid-prayer): silence right away.
                DndController.enable(ctx, w.key, w.ablution)
            } else {
                val takbirWithStart = w.takbirAt != null && w.takbirAt == w.start
                set(am, exact, w.start, pending(ctx, ACTION_START, slot, w, takbirWithStart))
                if (w.takbirAt != null && !takbirWithStart && w.takbirAt.isAfter(now)) {
                    set(am, exact, w.takbirAt, pending(ctx, ACTION_TAKBIR, slot, w))
                }
            }
            set(am, exact, w.end, pending(ctx, ACTION_END, slot, w))
            set(am, exact, w.end.plusSeconds(BACKUP_END_MIN * 60), pending(ctx, ACTION_BACKUP_END, slot, w))
            Log.i(TAG, "Scheduled ${w.key} ${w.start}..${w.end} slot=$slot exact=$exact")
        }
        return exact
    }

    private fun set(am: AlarmManager, exact: Boolean, at: Instant, pi: PendingIntent) {
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi)
        }
    }

    private fun requestCode(action: String, slot: Int) = 2000 + slot * 10 + when (action) {
        ACTION_START -> 1
        ACTION_END -> 2
        ACTION_BACKUP_END -> 3
        else -> 4
    }

    private fun intent(ctx: Context, action: String) = Intent(ctx, SilentModeReceiver::class.java).setAction(action)

    private fun pending(ctx: Context, action: String, slot: Int, w: SilentWindow, takbir: Boolean = false): PendingIntent {
        val i = intent(ctx, action)
            .putExtra(EXTRA_KEY, w.key)
            .putExtra(EXTRA_ABLUTION, w.ablution)
            .putExtra(EXTRA_TAKBIR, takbir)
        return PendingIntent.getBroadcast(ctx, requestCode(action, slot), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (slot in 0 until MAX_SLOTS) for (action in listOf(ACTION_START, ACTION_END, ACTION_BACKUP_END, ACTION_TAKBIR)) {
            PendingIntent.getBroadcast(ctx, requestCode(action, slot), intent(ctx, action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
                ?.let { am.cancel(it); it.cancel() }
        }
    }

    private fun dailyIntent(ctx: Context, flags: Int) =
        PendingIntent.getBroadcast(ctx, DAILY_REQUEST, intent(ctx, ACTION_DAILY), PendingIntent.FLAG_IMMUTABLE or flags)

    private fun scheduleDaily(ctx: Context, zone: ZoneId) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        val at = LocalDate.now(zone).plusDays(1).atTime(0, 1).atZone(zone).toInstant()
        set(am, exact, at, dailyIntent(ctx, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    private fun cancelDaily(ctx: Context) {
        dailyIntent(ctx, PendingIntent.FLAG_NO_CREATE)?.let {
            (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(it)
        }
    }
}
