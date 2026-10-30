package com.yahyaoui.prayermode

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.yahyaoui.prayermode.data.SettingsStore

/**
 * Turns Do Not Disturb on and off for prayer windows. Only undoes DND that it set itself,
 * and never overrides a DND mode the user or another app already enabled.
 */
object DndController {
    private const val TAG = "DndController"
    const val APP_CONTROLLED = "is_app_controlled_dnd_active"
    const val ACTIVE_KEY = "active_window_key"
    private val streams = mapOf(
        "music_volume" to AudioManager.STREAM_MUSIC,
        "notification_volume" to AudioManager.STREAM_NOTIFICATION,
        "ring_volume" to AudioManager.STREAM_RING,
        "system_volume" to AudioManager.STREAM_SYSTEM,
    )

    fun hasAccess(context: Context) =
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted

    fun isAppControlledActive(context: Context) = SettingsStore.getBoolean(context, APP_CONTROLLED)

    fun enable(context: Context, key: String, ablution: Boolean, notify: Boolean = SettingsStore.current(context).notifySilence): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (!nm.isNotificationPolicyAccessGranted) {
            NotificationHelper.notify(context, R.string.dnd_permission_title, context.getString(R.string.dnd_permission_message), 224)
            return false
        }
        val name = prayerLabel(context, key)
        return try {
            if (isAppControlledActive(context)) {
                SettingsStore.putString(context, ACTIVE_KEY, key)
                return true
            }
            if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                if (notify) NotificationHelper.notify(context, R.string.schedule_title, context.getString(R.string.dnd_already_on, name), 210)
                return true
            }
            val prefs = context.getSharedPreferences("pm_volumes", Context.MODE_PRIVATE).edit()
            streams.forEach { (k, s) -> prefs.putInt(k, am.getStreamVolume(s)) }
            prefs.apply()
            am.ringerMode = AudioManager.RINGER_MODE_SILENT
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
            streams.values.forEach { runCatching { am.setStreamVolume(it, 0, 0) } }
            SettingsStore.putBoolean(context, APP_CONTROLLED, true)
            SettingsStore.putString(context, ACTIVE_KEY, key)
            if (notify) {
                val msg = if (ablution) R.string.ablution_silent_mode_activation else R.string.silent_mode_activated_for
                NotificationHelper.notify(context, R.string.schedule_title, context.getString(msg, name), 220)
            }
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot enable DND: ${e.message}", e)
            false
        }
    }

    fun disable(context: Context, notify: Boolean = SettingsStore.current(context).notifySilence): Boolean {
        if (!isAppControlledActive(context)) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return try {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            am.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val saved = context.getSharedPreferences("pm_volumes", Context.MODE_PRIVATE)
            streams.forEach { (k, s) ->
                runCatching { am.setStreamVolume(s, saved.getInt(k, am.getStreamMaxVolume(s) / 2), 0) }
            }
            SettingsStore.putBoolean(context, APP_CONTROLLED, false)
            SettingsStore.putString(context, ACTIVE_KEY, null)
            if (notify) NotificationHelper.notify(context, R.string.schedule_title, context.getString(R.string.back_to_normal_mode), 221)
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot disable DND: ${e.message}", e)
            false
        }
    }

    fun prayerLabel(context: Context, key: String): String = context.getString(
        when (key) {
            "Fajr" -> R.string.fajr
            "Dhuhr" -> R.string.dhuhr
            "Asr" -> R.string.asr
            "Maghrib" -> R.string.maghrib
            "Isha" -> R.string.isha
            "Jumua" -> R.string.jumua
            "Taraweeh" -> R.string.taraweeh
            "Tahajjud" -> R.string.tahajjud
            "Eid" -> R.string.eid
            "Sunrise" -> R.string.sunrise
            else -> R.string.prayer_name_unknown
        }
    )
}
