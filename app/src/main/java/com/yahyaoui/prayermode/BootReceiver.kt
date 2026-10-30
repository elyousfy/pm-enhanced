package com.yahyaoui.prayermode

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-plans after reboot, app update, or any clock / time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && DndController.isAppControlledActive(context)) {
            // A window may have ended while the phone was off.
            DndController.disable(context, notify = false)
        }
        RebuildWorker.enqueue(context, intent.action ?: "system")
    }
}
