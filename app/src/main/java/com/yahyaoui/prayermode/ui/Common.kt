package com.yahyaoui.prayermode.ui

import android.content.Context
import com.yahyaoui.prayermode.DndController
import com.yahyaoui.prayermode.R
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.Prayer
import com.yahyaoui.prayermode.core.PrayerConfig
import com.yahyaoui.prayermode.data.SettingsStore
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Saves a config change; SettingsStore reschedules alarms. */
typealias Update = ((AppConfig) -> AppConfig) -> Unit

fun AppConfig.withPrayer(p: Prayer, change: (PrayerConfig) -> PrayerConfig) =
    copy(settings = settings.copy(prayers = settings.prayers + (p to change(settings.prayer(p)))))

fun setMaster(ctx: Context, on: Boolean) {
    SettingsStore.update(ctx) { it.copy(settings = it.settings.copy(masterOn = on)) }
    if (!on && DndController.isAppControlledActive(ctx)) DndController.disable(ctx, notify = false)
}

fun weekOrder(sundayFirst: Boolean): List<DayOfWeek> =
    if (sundayFirst) listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.dropLast(1) else DayOfWeek.entries

fun dayShort(d: DayOfWeek, locale: Locale) = d.getDisplayName(TextStyle.SHORT, locale)
fun dayFull(d: DayOfWeek, locale: Locale) = d.getDisplayName(TextStyle.FULL, locale)

fun daysSummary(res: android.content.res.Resources, days: Set<DayOfWeek>, sundayFirst: Boolean, locale: Locale) = when {
    days.size == 7 -> res.getString(R.string.every_day)
    days.isEmpty() -> res.getString(R.string.no_days)
    else -> weekOrder(sundayFirst).filter { it in days }.joinToString(", ") { dayShort(it, locale) }
}

/** App locale, re-read when the configuration changes. */
@androidx.compose.runtime.Composable
fun currentLocale(): Locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
