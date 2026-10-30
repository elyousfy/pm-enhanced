package com.yahyaoui.prayermode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.yahyaoui.prayermode.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles prayer alarms directly (no WorkManager hop), so DND switches at the exact minute.
 * Takbir playback runs under goAsync() before silencing.
 */
class SilentModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ctx = context.applicationContext
        val key = intent.getStringExtra(PrayerScheduler.EXTRA_KEY) ?: "unknown"
        Log.i("SilentModeReceiver", "${intent.action} for $key")
        when (intent.action) {
            PrayerScheduler.ACTION_START -> {
                if (!PrayerScheduler.isPlannedNow(ctx, key)) return
                val ablution = intent.getBooleanExtra(PrayerScheduler.EXTRA_ABLUTION, false)
                if (intent.getBooleanExtra(PrayerScheduler.EXTRA_TAKBIR, false) && canPlayTakbir(ctx)) {
                    runAsync { playTakbir(ctx); DndController.enable(ctx, key, ablution) }
                } else {
                    DndController.enable(ctx, key, ablution)
                }
            }
            PrayerScheduler.ACTION_TAKBIR -> if (canPlayTakbir(ctx) && SettingsStore.current(ctx).settings.masterOn) runAsync { playTakbir(ctx) }
            PrayerScheduler.ACTION_END, PrayerScheduler.ACTION_BACKUP_END -> {
                val active = SettingsStore.getString(ctx, DndController.ACTIVE_KEY)
                // Do not end a window that a later prayer has since taken over.
                if (active == null || active == key) DndController.disable(ctx, notify = intent.action == PrayerScheduler.ACTION_END && SettingsStore.current(ctx).notifySilence)
            }
            PrayerScheduler.ACTION_DAILY -> RebuildWorker.enqueue(ctx, "daily")
        }
    }

    private fun canPlayTakbir(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val inCall = am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
        return SettingsStore.current(ctx).settings.takbir && !inCall
    }

    private suspend fun playTakbir(ctx: Context) {
        withTimeoutOrNull(45_000) { AudioPlayerHelper(ctx).playAudioFromRaw(R.raw.takbir) }
    }

    private fun runAsync(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { block() } finally { pending.finish() }
        }
    }
}
