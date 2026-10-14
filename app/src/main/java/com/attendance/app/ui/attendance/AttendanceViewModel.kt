package com.attendance.app.ui.attendance

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferences
) : ViewModel() {
    val attendance = repository.attendance
    val isRefreshing: StateFlow<Boolean> = repository.isRefreshing

    val notificationThreshold: Int
        get() = prefs.notificationThreshold

    fun refresh() {
        val studentId = prefs.studentId ?: return
        val password = prefs.password ?: return
        viewModelScope.launch {
            repository.fetchAttendance(studentId, password)
        }
    }

    var searchQuery = mutableStateOf("")
        private set

    var currentSortOrder = mutableStateOf(SortOrder.ALPHABETICAL)
        private set

    val simulatedLeaves = mutableStateListOf<Long>()
    val simulatedHolidays = mutableStateListOf<Long>()

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        currentSortOrder.value = order
    }

    fun addSimulatedLeave(millis: Long) {
        if (!simulatedLeaves.contains(millis)) {
            simulatedLeaves.add(millis)
            simulatedHolidays.remove(millis)
        }
    }

    fun addSimulatedHoliday(millis: Long) {
        if (!simulatedHolidays.contains(millis)) {
            simulatedHolidays.add(millis)
            simulatedLeaves.remove(millis)
        }
    }

    fun removeSimulatedDate(millis: Long) {
        simulatedLeaves.remove(millis)
        simulatedHolidays.remove(millis)
    }

    fun resetSimulation() {
        simulatedLeaves.clear()
        simulatedHolidays.clear()
    }

    enum class SortOrder {
        ALPHABETICAL,
        PERCENTAGE_ASC,
        PERCENTAGE_DESC
    }
}
