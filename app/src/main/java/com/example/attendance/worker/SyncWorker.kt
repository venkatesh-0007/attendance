package com.example.attendance.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val studentId = prefs.studentId
        val password = prefs.password

        if (studentId.isNullOrBlank() || password.isNullOrBlank()) {
            return@withContext Result.failure()
        }

        val result = repository.fetchAttendance(studentId, password)
        if (result.isSuccess) {
            val response = result.getOrNull()
            response?.overall_attendance?.let { percentage ->
                val threshold = prefs.notificationThreshold
                if (percentage < threshold) {
                    NotificationHelper.showLowAttendanceNotification(context, percentage, threshold)
                }
            }

            // Trigger Jetpack Glance home widget update if possible
            try {
                com.example.attendance.widget.AttendanceWidget.updateAll(context)
            } catch (e: Exception) {
                // Widget class might not be fully loaded yet
                e.printStackTrace()
            }

            Result.success()
        } else {
            Result.retry()
        }
    }
}
