package com.attendance.app.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferences
) : ViewModel() {

    var darkMode by mutableStateOf(prefs.darkMode)
        private set

    var accentColor by mutableStateOf(prefs.accentColor)
        private set

    var notificationThreshold by mutableStateOf(prefs.notificationThreshold)
        private set

    var refreshIntervalMinutes by mutableStateOf(prefs.refreshIntervalMinutes)
        private set

    var savedAccounts by mutableStateOf(repository.getSavedAccounts())
        private set

    var currentStudentId by mutableStateOf(prefs.studentId)
        private set

    var autoSyncActiveHoursOnly by mutableStateOf(prefs.autoSyncActiveHoursOnly)
        private set

    var activeStartHour by mutableStateOf(prefs.activeStartHour)
        private set

    var activeEndHour by mutableStateOf(prefs.activeEndHour)
        private set

    val attendanceStateFlow = repository.attendance

    var isRefreshing by mutableStateOf(false)
        private set

    val lastUpdated: StateFlow<Long> = repository.lastUpdated

    fun updateDarkMode(mode: String, context: Context) {
        prefs.darkMode = mode
        darkMode = mode
        refreshWidgets(context)
    }

    fun updateAccentColor(hex: String?, context: Context) {
        prefs.accentColor = hex
        accentColor = hex
        refreshWidgets(context)
    }

    fun updateNotificationThreshold(threshold: Int, context: Context) {
        prefs.notificationThreshold = threshold
        notificationThreshold = threshold
        refreshWidgets(context)
    }

    fun updateRefreshInterval(minutes: Int, context: Context) {
        prefs.refreshIntervalMinutes = minutes
        refreshIntervalMinutes = minutes
        rescheduleWorker(context)
    }

    fun updateAutoSyncActiveHoursOnly(enabled: Boolean) {
        prefs.autoSyncActiveHoursOnly = enabled
        autoSyncActiveHoursOnly = enabled
    }

    fun updateActiveHours(startHour: Int, endHour: Int) {
        prefs.activeStartHour = startHour
        prefs.activeEndHour = endHour
        activeStartHour = startHour
        activeEndHour = endHour
    }

    private fun refreshWidgets(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.attendance.app.widget.WidgetUpdater.updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun switchAccount(studentId: String, context: Context) {
        repository.switchAccount(studentId)
        currentStudentId = prefs.studentId
        savedAccounts = repository.getSavedAccounts()
        refreshWidgets(context)
    }

    fun removeAccount(studentId: String, context: Context) {
        repository.removeAccount(studentId)
        savedAccounts = repository.getSavedAccounts()
        currentStudentId = prefs.studentId
        refreshWidgets(context)
    }

    fun updateAccountCustomName(studentId: String, customName: String, context: Context) {
        repository.updateAccountCustomName(studentId, customName)
        savedAccounts = repository.getSavedAccounts()
        refreshWidgets(context)
    }

    fun logout() {
        repository.logout()
    }

    fun clearCache() {
        repository.clearCache()
    }

    fun refresh() {
        val studentId = prefs.studentId ?: return
        val password = prefs.password ?: return

        viewModelScope.launch {
            isRefreshing = true
            repository.fetchAttendance(studentId, password)
            isRefreshing = false
        }
    }

    private fun rescheduleWorker(context: Context) {
        val intervalMinutes = prefs.refreshIntervalMinutes.toLong().coerceAtLeast(15L)
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AttendanceSyncWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }
}
