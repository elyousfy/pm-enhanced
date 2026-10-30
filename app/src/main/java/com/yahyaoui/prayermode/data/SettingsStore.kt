package com.yahyaoui.prayermode.data

import android.content.Context
import androidx.core.content.edit
import com.yahyaoui.prayermode.core.AppConfig
import com.yahyaoui.prayermode.core.SettingsCodec
import com.yahyaoui.prayermode.RebuildWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single source of truth for user configuration. Every change re-plans today's schedule. */
object SettingsStore {
    private const val PREFS = "pm_enhanced"
    private const val KEY = "config_v1"

    private var flow: MutableStateFlow<AppConfig>? = null

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun config(context: Context): StateFlow<AppConfig> = synchronized(this) {
        flow ?: MutableStateFlow(SettingsCodec.decode(prefs(context).getString(KEY, null))).also { flow = it }
    }.asStateFlow()

    fun current(context: Context): AppConfig = config(context).value

    fun update(context: Context, change: (AppConfig) -> AppConfig) {
        val next = synchronized(this) {
            config(context)
            val f = flow!!
            change(f.value).also { f.value = it }
        }
        prefs(context).edit { putString(KEY, SettingsCodec.encode(next)) }
        RebuildWorker.enqueue(context, "settings")
    }

    // Small app-state values that are not user configuration.
    fun getBoolean(context: Context, key: String, default: Boolean = false) = prefs(context).getBoolean(key, default)
    fun putBoolean(context: Context, key: String, value: Boolean) = prefs(context).edit { putBoolean(key, value) }
    fun getString(context: Context, key: String): String? = prefs(context).getString(key, null)
    fun putString(context: Context, key: String, value: String?) = prefs(context).edit { putString(key, value) }
}
