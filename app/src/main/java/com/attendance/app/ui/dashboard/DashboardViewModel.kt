package com.attendance.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferences
) : ViewModel() {

    val attendance = repository.attendance

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val lastUpdated: Long
        get() = prefs.lastUpdated

    val notificationThreshold: Int
        get() = prefs.notificationThreshold

    val currentAccountName: String?
        get() {
            val id = prefs.studentId ?: return null
            val account = repository.getSavedAccounts().find { it.studentId == id }
            return account?.displayName ?: attendance.value?.studentName ?: attendance.value?.rollNumber
        }

    fun refresh() {
        val studentId = prefs.studentId ?: return
        val password = prefs.password ?: return

        viewModelScope.launch {
            _isRefreshing.value = true
            repository.fetchAttendance(studentId, password)
            _isRefreshing.value = false
        }
    }
}
