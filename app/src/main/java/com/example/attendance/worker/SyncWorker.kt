package com.example.attendance.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

        if (!isNetworkAvailable()) {
            return@withContext Result.retry()
        }

        val result = repository.fetchAttendance(studentId, password)
        if (result.isSuccess) {
            val response = result.getOrNull()
            response?.overallPercentage?.let { percentage ->
                val threshold = prefs.notificationThreshold
                if (percentage < threshold) {
                    NotificationHelper.showLowAttendanceNotification(context, percentage, threshold)
                }
            }

            // Trigger updates across all home screen widgets
            try {
                com.example.attendance.widget.WidgetUpdater.updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success()
        } else {
            Result.retry()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
