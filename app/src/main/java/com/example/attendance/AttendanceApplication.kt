package com.example.attendance

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.worker.SyncWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class AttendanceApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initBackgroundSync()
    }

    private fun initBackgroundSync() {
        val prefs = SecurePreferences(applicationContext)
        if (prefs.hasCredentials) {
            val interval = prefs.refreshIntervalMinutes.toLong().coerceAtLeast(15L)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(interval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "attendance_sync_work",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
