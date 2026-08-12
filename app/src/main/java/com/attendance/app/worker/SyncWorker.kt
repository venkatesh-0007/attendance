package com.attendance.app.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.util.NotificationHelper
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
        if (!isNetworkAvailable()) {
            return@withContext Result.retry()
        }

        val accounts = repository.getSavedAccounts()
        val accountsToSync = if (accounts.isNotEmpty()) accounts else {
            val id = prefs.studentId
            val pass = prefs.password
            if (!id.isNullOrBlank() && !pass.isNullOrBlank()) {
                listOf(com.attendance.app.data.model.UserAccount(id, pass))
            } else emptyList()
        }

        if (accountsToSync.isEmpty()) {
            return@withContext Result.failure()
        }

        var anySuccess = false
        for (account in accountsToSync) {
            val result = repository.fetchAttendance(account.studentId, account.password)
            if (result.isSuccess) {
                anySuccess = true
                val response = result.getOrNull()
                response?.overallPercentage?.let { percentage ->
                    val threshold = prefs.notificationThreshold
                    if (percentage < threshold && account.studentId == prefs.studentId) {
                        NotificationHelper.showLowAttendanceNotification(context, percentage, threshold)
                    }
                }
            }
        }

        // Trigger updates across all home screen widgets
        try {
            com.attendance.app.widget.WidgetUpdater.updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (anySuccess) Result.success() else Result.retry()
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
