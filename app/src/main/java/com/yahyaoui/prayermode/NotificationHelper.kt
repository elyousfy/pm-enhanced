package com.yahyaoui.prayermode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import com.yahyaoui.prayermode.data.SettingsStore
import com.yahyaoui.prayermode.ui.hm
import java.time.Instant

object NotificationHelper {
    const val CHANNEL_ID = "prayer_channel"
    const val WORK_CHANNEL_ID = "pm_background"
    /** Silence started/ended and prayer time share this id, so only one notification shows at a time. */
    const val STATUS_ID = 220

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(WORK_CHANNEL_ID, context.getString(R.string.schedule_title), NotificationManager.IMPORTANCE_MIN))
    }

    fun notify(context: Context, titleResId: Int, message: String, notificationId: Int) =
        notify(context, context.getString(titleResId), message, notificationId)

    fun notify(context: Context, title: String, message: String, notificationId: Int) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pm_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(notificationId, notification) }
    }

    /** "Time for Dhuhr", with the time and whether the phone is silent right now. */
    fun prayerTime(context: Context, key: String, at: Instant) {
        val c = SettingsStore.current(context)
        val is24 = when (c.clock) { "24" -> true; "12" -> false; else -> DateFormat.is24HourFormat(context) }
        val time = hm(at, PrayerScheduler.zoneOf(c), is24)
        val message = if (DndController.isAppControlledActive(context)) "$time · ${context.getString(R.string.widget_silent_now)}" else time
        notify(context, context.getString(R.string.prayer_time_title, DndController.prayerLabel(context, key)), message, STATUS_ID)
    }

    fun workNotification(context: Context) = run {
        ensureChannels(context)
        NotificationCompat.Builder(context, WORK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pm_notification)
            .setContentTitle(context.getString(R.string.schedule_title))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
