package com.yahyaoui.prayermode.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

enum class Perm { DND, ALARMS, NOTIFICATIONS, BATTERY }

object Permissions {
    fun granted(ctx: Context, p: Perm): Boolean = when (p) {
        Perm.DND -> (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).isNotificationPolicyAccessGranted
        Perm.ALARMS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        Perm.NOTIFICATIONS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        Perm.BATTERY -> (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    }

    /** System screen for [p]; null for notifications (runtime request instead) when it can be requested. */
    fun settingsIntent(ctx: Context, p: Perm): Intent = when (p) {
        Perm.DND -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        Perm.ALARMS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${ctx.packageName}".toUri())
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${ctx.packageName}".toUri())
        Perm.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${ctx.packageName}".toUri())
        Perm.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${ctx.packageName}".toUri())
    }
}

/** Permission state, re-read every time the screen resumes (grants happen in system settings). */
@androidx.compose.runtime.Composable
fun rememberPermissions(): Map<Perm, Boolean> {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var state by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(Perm.entries.associateWith { Permissions.granted(ctx, it) }) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        state = Perm.entries.associateWith { Permissions.granted(ctx, it) }
        onPauseOrDispose { }
    }
    return state
}
