package com.yahyaoui.prayermode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "prayer_channel"
    const val WORK_CHANNEL_ID = "pm_background"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(WORK_CHANNEL_ID, context.getString(R.string.schedule_title), NotificationManager.IMPORTANCE_MIN))
    }

    fun notify(context: Context, titleResId: Int, message: String, notificationId: Int) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_pm_notification)
            .setContentTitle(context.getString(titleResId))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(notificationId, notification) }
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
