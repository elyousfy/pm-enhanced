package com.yahyaoui.prayermode

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Re-plans today's silent windows. Retries with backoff while prayer times cannot be loaded. */
class RebuildWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val outcome = PrayerScheduler.rebuild(applicationContext)
        if (outcome is PrayerScheduler.Outcome.NoData) {
            if (runAttemptCount == 3) {
                NotificationHelper.notify(applicationContext, R.string.fetch_title, applicationContext.getString(R.string.fetch_failed), 50)
            }
            return Result.retry()
        }
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val n = NotificationHelper.workNotification(applicationContext)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(77, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else ForegroundInfo(77, n)
    }

    companion object {
        private const val NAME = "pm_rebuild"

        fun enqueue(context: Context, reason: String) {
            val request = OneTimeWorkRequestBuilder<RebuildWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(reason)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
