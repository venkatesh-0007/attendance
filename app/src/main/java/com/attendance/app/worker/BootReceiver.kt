package com.attendance.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.attendance.app.data.local.SecurePreferences
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = SecurePreferences(context)
            if (prefs.hasCredentials) {
                val interval = prefs.refreshIntervalMinutes.toLong().coerceAtLeast(15L)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(interval, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "attendance_sync_work",
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
            }
        }
    }
}
