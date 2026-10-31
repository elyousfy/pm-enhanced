package com.yahyaoui.prayermode

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.SystemClock
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.yahyaoui.prayermode.data.SettingsStore
import com.yahyaoui.prayermode.ui.Accent
import com.yahyaoui.prayermode.ui.Palette
import com.yahyaoui.prayermode.ui.PmThemeId
import com.yahyaoui.prayermode.ui.TodayData
import com.yahyaoui.prayermode.ui.TodayState
import com.yahyaoui.prayermode.ui.dayPlan
import com.yahyaoui.prayermode.ui.heroInfo
import com.yahyaoui.prayermode.ui.hm
import com.yahyaoui.prayermode.ui.loadToday
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.format.TextStyle

/**
 * 2x2 home-screen widgets in four styles: next prayer, a live countdown with seconds, the city
 * and whether auto-silence is on, off or silencing now. Colours follow the app theme.
 * The Chronometer counts down on its own; an alarm at the next change (adhan, silence start or
 * end) redraws. Also redrawn after every rebuild (settings change, daily, boot, clock change).
 */
abstract class PrayerWidget(private val style: Style) : AppWidgetProvider() {
    class Classic : PrayerWidget(Style.CLASSIC)
    class Mihrab : PrayerWidget(Style.MIHRAB)
    class Band : PrayerWidget(Style.BAND)
    class Ring : PrayerWidget(Style.RING)
    class Wide : PrayerWidget(Style.WIDE)

    enum class Style(val layout: Int) {
        CLASSIC(R.layout.widget_style_classic),
        MIHRAB(R.layout.widget_style_mihrab),
        BAND(R.layout.widget_style_band),
        RING(R.layout.widget_style_ring),
        WIDE(R.layout.widget_style_wide);

        val provider: Class<out PrayerWidget>
            get() = when (this) {
                CLASSIC -> Classic::class.java
                MIHRAB -> Mihrab::class.java
                BAND -> Band::class.java
                RING -> Ring::class.java
                WIDE -> Wide::class.java
            }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refreshAsync(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TICK) refreshAsync(context)
    }

    override fun onDisabled(context: Context) {
        // Last widget of this style removed; keep the alarm while other styles remain.
        if (Style.entries.none { ids(context, it).isNotEmpty() }) {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(tickIntent(context))
        }
    }

    private fun refreshAsync(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { update(context) } finally { pending.finish() }
        }
    }

    private enum class Status { ON, OFF, SILENT }

    /** Everything a style shows; built once per refresh. */
    private class Model(
        val status: Status,
        val name: String,
        val label: String,
        val target: Instant,
        val time: String,
        val line: String,
        val progress: Float,
        val date: String,
        val nextChange: Instant,
        /** Wide style: (name, time) for Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha; [highlight] is the current one. */
        val rows: List<Pair<String, String>>,
        val highlight: Int,
    )

    companion object {
        private const val ACTION_TICK = "com.yahyaoui.prayermode.WIDGET_TICK"
        private const val TICK_REQUEST = 3124
        private const val RETRY_MS = 30 * 60_000L
        /** The ring creeps forward, so ring widgets redraw at least this often while the phone is awake. */
        private const val RING_STEP_MS = 10 * 60_000L

        private val ROW_NAMES = intArrayOf(R.id.widget_r0_name, R.id.widget_r1_name, R.id.widget_r2_name, R.id.widget_r3_name, R.id.widget_r4_name, R.id.widget_r5_name)
        private val ROW_TIMES = intArrayOf(R.id.widget_r0_time, R.id.widget_r1_time, R.id.widget_r2_time, R.id.widget_r3_time, R.id.widget_r4_time, R.id.widget_r5_time)

        private fun ids(ctx: Context, s: Style) =
            AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, s.provider))

        suspend fun update(context: Context) {
            val ctx = context.applicationContext
            val manager = AppWidgetManager.getInstance(ctx)
            val placed = Style.entries.associateWith { ids(ctx, it) }.filterValues { it.isNotEmpty() }
            if (placed.isEmpty()) return

            val config = SettingsStore.current(ctx)
            val hasCity = config.location.city.isNotBlank() || config.location.latitude != null
            val state = if (hasCity) withTimeoutOrNull(8_000) { loadToday(ctx, config) } else null
            val model = (state as? TodayState.Ready)?.let { model(ctx, it.data, config) }

            val accent = Accent.from(config.accent)
            val explicit = PmThemeId.entries.firstOrNull { it.id == config.theme }
            val night = (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val resolved = (explicit ?: PmThemeId.resolve(config.theme, night)).palette(accent)
            // "system" on Android 12+ hands both palettes to the launcher so the widget follows dark mode live.
            val both = explicit == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val light = if (both) PmThemeId.LIGHT.palette(accent) else resolved
            val dark = if (both) PmThemeId.MIDNIGHT.palette(accent) else resolved

            for ((style, ids) in placed) {
                val v = RemoteViews(ctx.packageName, style.layout)
                v.setOnClickPendingIntent(
                    android.R.id.background,
                    PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE),
                )
                fill(ctx, v, style, model, config.location.city)
                paint(ctx, v, style, model?.status ?: Status.OFF, model?.highlight ?: -1, light, dark)
                manager.updateAppWidget(ids, v)
            }

            when {
                model != null -> {
                    val next = model.nextChange.toEpochMilli() + 1_000
                    scheduleTick(ctx, if (Style.RING in placed) minOf(next, System.currentTimeMillis() + RING_STEP_MS) else next)
                }
                hasCity -> scheduleTick(ctx, System.currentTimeMillis() + RETRY_MS)
            }
        }

        private fun model(ctx: Context, data: TodayData, config: com.yahyaoui.prayermode.core.AppConfig): Model {
            val now = Instant.now()
            val is24 = when (config.clock) { "24" -> true; "12" -> false; else -> DateFormat.is24HourFormat(ctx) }
            fun t(i: Instant) = hm(i, data.zone, is24)
            // The next prayer itself (ignoring silence), and what silence is doing right now.
            val next = heroInfo(data, config.copy(settings = config.settings.copy(masterOn = false)), now)
            val live = heroInfo(data, config, now)
            val status = when {
                !config.settings.masterOn -> Status.OFF
                live.silentNow -> Status.SILENT
                else -> Status.ON
            }
            val locale = ctx.resources.configuration.locales[0]
            val months = ctx.resources.getStringArray(R.array.hijri_months)
            val day = data.day
            val weekday = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            val date = if (day.hijriMonth in 1..12) "$weekday · ${day.hijriDay} ${months[day.hijriMonth - 1]}" else weekday

            // Times list for the wide style: tomorrow's once Isha has passed.
            val listDay = if (next.tomorrow && data.tomorrow != null) data.tomorrow else day
            val slots = dayPlan(listDay, null, config, data.zone).slots
            val rows = slots.map { DndController.prayerLabel(ctx, it.key) to t(it.at) }.toMutableList()
            rows.add(1, ctx.getString(R.string.sunrise) to t(listDay.date.atTime(listDay.sunrise).atZone(data.zone).toInstant()))
            fun rowOf(key: String, at: Instant): Int {
                val i = slots.indexOfFirst { it.key == key && it.at == at }
                return if (i < 0) -1 else if (i == 0) 0 else i + 1
            }

            if (status == Status.SILENT) {
                val name = DndController.prayerLabel(ctx, live.key)
                val end = t(live.target)
                val silenced = live.window?.let { rowOf(it.key, it.adhan) } ?: -1
                return Model(status, name, ctx.getString(R.string.widget_silent_now), live.target, end,
                    ctx.getString(R.string.widget_silent_until, end), live.progress, date, live.target, rows, silenced)
            }
            val name = DndController.prayerLabel(ctx, next.key)
            val start = next.window?.start
            val line = when {
                status == Status.OFF -> ctx.getString(R.string.widget_auto_off)
                start != null -> ctx.getString(R.string.widget_silences_at, t(start))
                else -> ctx.getString(R.string.widget_not_silenced, name)
            }
            val change = if (status == Status.ON && start != null && start.isAfter(now) && start.isBefore(next.target)) start else next.target
            return Model(status, name, ctx.getString(R.string.widget_until, name), next.target, t(next.target), line, next.progress, date, change,
                rows, rowOf(next.key, next.target))
        }

        /** Texts, countdown and the ring. */
        private fun fill(ctx: Context, v: RemoteViews, style: Style, m: Model?, city: String) {
            v.setTextViewText(R.id.widget_city, city)
            v.setViewVisibility(R.id.widget_pin, if (city.isBlank()) View.GONE else View.VISIBLE)
            if (m == null) {
                v.setTextViewText(R.id.widget_label, ctx.getString(R.string.widget_no_times))
                v.setViewVisibility(R.id.widget_countdown, View.INVISIBLE)
                if (style == Style.CLASSIC || style == Style.WIDE) v.setTextViewText(R.id.widget_date, "")
                if (style == Style.WIDE) for (i in 0..5) { v.setTextViewText(ROW_NAMES[i], ""); v.setTextViewText(ROW_TIMES[i], "") }
                if (style == Style.MIHRAB || style == Style.BAND) v.setTextViewText(R.id.widget_status, "")
                if (style == Style.BAND) v.setTextViewText(R.id.widget_eyebrow, "")
                if (style == Style.RING) { v.setTextViewText(R.id.widget_time, ""); v.setImageViewBitmap(R.id.widget_ring, ring(ctx, 0f)) }
                pill(ctx, v, style, Status.OFF)
                return
            }
            val left = m.target.toEpochMilli() - System.currentTimeMillis()
            v.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + left, null, true)
            v.setChronometerCountDown(R.id.widget_countdown, true)
            v.setViewVisibility(R.id.widget_countdown, View.VISIBLE)
            pill(ctx, v, style, m.status)
            when (style) {
                Style.CLASSIC -> {
                    v.setTextViewText(R.id.widget_label, m.label)
                    v.setTextViewText(R.id.widget_date, m.date)
                }
                Style.WIDE -> {
                    v.setTextViewText(R.id.widget_label, m.label)
                    v.setTextViewText(R.id.widget_date, m.date)
                    m.rows.forEachIndexed { i, (name, time) ->
                        v.setTextViewText(ROW_NAMES[i], name)
                        v.setTextViewText(ROW_TIMES[i], time)
                    }
                }
                Style.MIHRAB -> {
                    v.setTextViewText(R.id.widget_label, m.name)
                    v.setTextViewText(R.id.widget_status, m.line)
                }
                Style.BAND -> {
                    v.setTextViewText(R.id.widget_eyebrow, if (m.status == Status.SILENT) m.label else ctx.getString(R.string.widget_next, m.time))
                    v.setTextViewText(R.id.widget_label, m.name)
                    v.setTextViewText(R.id.widget_status, m.line)
                    v.setImageViewResource(R.id.widget_band_icon, if (m.status == Status.OFF) R.drawable.ic_widget_off else R.drawable.ic_widget_on)
                    v.setViewVisibility(R.id.widget_band_bg, if (m.status == Status.OFF) View.GONE else View.VISIBLE)
                    v.setViewVisibility(R.id.widget_band_line, if (m.status == Status.OFF) View.VISIBLE else View.GONE)
                }
                Style.RING -> {
                    v.setTextViewText(R.id.widget_label, m.name)
                    v.setTextViewText(R.id.widget_time, if (m.status == Status.SILENT) m.line else m.time)
                    v.setImageViewBitmap(R.id.widget_ring, ring(ctx, m.progress))
                }
            }
        }

        private fun pill(ctx: Context, v: RemoteViews, style: Style, s: Status) {
            if (style == Style.BAND) return // the band shows the status itself
            v.setTextViewText(R.id.widget_pill_text, ctx.getString(when (s) { Status.ON -> R.string.widget_on; Status.OFF -> R.string.widget_off; Status.SILENT -> R.string.widget_silent }))
            v.setImageViewResource(R.id.widget_pill_bg, if (s == Status.OFF) R.drawable.widget_pill_outline else R.drawable.widget_pill)
            v.setImageViewResource(R.id.widget_dot, if (s == Status.OFF) R.drawable.widget_dot_ring else R.drawable.widget_dot)
        }

        /** White progress arc, tinted like everything else. */
        private fun ring(ctx: Context, progress: Float): Bitmap {
            val d = ctx.resources.displayMetrics.density
            val size = (128 * d).toInt()
            val stroke = 5 * d
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                color = android.graphics.Color.WHITE
            }
            val inset = stroke / 2
            Canvas(bmp).drawArc(RectF(inset, inset, size - inset, size - inset), -90f, 360f * progress.coerceIn(0f, 1f), false, p)
            return bmp
        }

        /** Applies the theme; [light] and [dark] differ only for the phone-following theme on Android 12+. */
        private fun paint(ctx: Context, v: RemoteViews, style: Style, s: Status, highlight: Int, light: Palette, dark: Palette) {
            fun set(id: Int, method: String, c: (Palette) -> Color) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) v.setColorInt(id, method, c(light).toArgb(), c(dark).toArgb())
                else v.setInt(id, method, c(light).toArgb())
            }
            fun wash(p: Palette) = p.accText.copy(alpha = if (p.isLight) 0.10f else 0.16f).compositeOver(p.bg)
            val countdown: (Palette) -> Color = when {
                s == Status.OFF -> { p -> p.sub }
                style == Style.BAND -> { p -> p.text }
                else -> { p -> p.accText }
            }

            // Background: a top-to-bottom fade between the two darkest (or lightest) surfaces, then an
            // glow from the bottom corner in the selected accent's hue. The black-and-white accent has no
            // hue, so it gets a faint neutral glow.
            set(R.id.widget_bg, "setColorFilter") { if (it.isLight) it.surface else it.bg }
            set(R.id.widget_shade, "setColorFilter") { if (it.isLight) it.bg else it.surface }
            set(R.id.widget_glow, "setColorFilter") {
                val neutral = it.acc.red == it.acc.green && it.acc.green == it.acc.blue
                when {
                    neutral -> if (it.isLight) it.surface else lerp(it.bg, it.acc, 0.25f)
                    it.isLight -> lerp(it.bg, it.acc, 0.35f)
                    else -> it.acc
                }
            }
            set(R.id.widget_label, "setTextColor") { it.text }
            set(R.id.widget_countdown, "setTextColor", countdown)
            val bright = style == Style.CLASSIC || style == Style.WIDE
            set(R.id.widget_city, "setTextColor") { if (bright) it.text else it.sub }
            set(R.id.widget_pin, "setColorFilter") { if (bright) it.text else it.sub }
            if (style != Style.RING) set(R.id.widget_mark, "setColorFilter") { it.accText }

            when (style) {
                Style.CLASSIC, Style.WIDE -> {
                    set(R.id.widget_logo, "setColorFilter") { it.text }
                    set(R.id.widget_brand, "setTextColor") { it.sub }
                    set(R.id.widget_date, "setTextColor") { it.sub }
                    if (style == Style.WIDE) for (i in 0..5) {
                        val row: (Palette) -> Color = if (i == highlight && s != Status.OFF) { p -> p.accText } else { p -> p.text }
                        set(ROW_NAMES[i], "setTextColor", row)
                        set(ROW_TIMES[i], "setTextColor", row)
                    }
                }
                Style.MIHRAB -> set(R.id.widget_status, "setTextColor") { it.sub }
                Style.BAND -> {
                    set(R.id.widget_eyebrow, "setTextColor") { it.sub }
                    val fg: (Palette) -> Color = when (s) { Status.ON -> { p -> p.accText }; Status.OFF -> { p -> p.faint }; Status.SILENT -> { p -> p.onAcc } }
                    set(R.id.widget_band_bg, "setColorFilter") { if (s == Status.SILENT) it.acc else wash(it) }
                    set(R.id.widget_band_line, "setColorFilter") { it.line2 }
                    set(R.id.widget_band_icon, "setColorFilter", fg)
                    set(R.id.widget_status, "setTextColor", fg)
                }
                Style.RING -> {
                    set(R.id.widget_logo, "setColorFilter") { it.sub }
                    set(R.id.widget_time, "setTextColor") { it.sub }
                    set(R.id.widget_ring_track, "setColorFilter") { it.line2 }
                    set(R.id.widget_ring, "setColorFilter") { if (s == Status.OFF) it.faint else it.accText }
                }
            }

            if (style != Style.BAND) {
                val fg: (Palette) -> Color = when (s) { Status.ON -> { p -> p.accText }; Status.OFF -> { p -> p.faint }; Status.SILENT -> { p -> p.onAcc } }
                set(R.id.widget_pill_bg, "setColorFilter") { when (s) { Status.ON -> wash(it); Status.OFF -> it.line2; Status.SILENT -> it.acc } }
                set(R.id.widget_dot, "setColorFilter", fg)
                set(R.id.widget_pill_text, "setTextColor", fg)
            }
        }

        private fun tickIntent(ctx: Context) = PendingIntent.getBroadcast(
            ctx, TICK_REQUEST, Intent(ctx, Classic::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /** Non-wakeup alarm: it fires once the phone is awake, which is when the widget can be seen. */
        private fun scheduleTick(ctx: Context, atMs: Long) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            try {
                if (exact) am.setExact(AlarmManager.RTC, atMs, tickIntent(ctx)) else am.set(AlarmManager.RTC, atMs, tickIntent(ctx))
            } catch (e: SecurityException) {
                am.set(AlarmManager.RTC, atMs, tickIntent(ctx))
            }
        }
    }
}
