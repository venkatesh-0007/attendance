package com.example.attendance.data.repository

import com.example.attendance.data.api.AttendanceApi
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.AttendanceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AttendanceRepository(
    private val api: AttendanceApi,
    private val prefs: SecurePreferences,
    private val json: Json
) {
    private val _attendance = MutableStateFlow<AttendanceResponse?>(null)
    val attendance: StateFlow<AttendanceResponse?> = _attendance.asStateFlow()

    init {
        loadCachedData()
    }

    private fun loadCachedData() {
        val cachedJson = prefs.attendanceCache
        if (!cachedJson.isNullOrBlank()) {
            try {
                _attendance.value = json.decodeFromString<AttendanceResponse>(cachedJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getCachedAttendance(): AttendanceResponse? {
        return _attendance.value
    }

    suspend fun fetchAttendance(studentId: String, password: String): Result<AttendanceResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAttendance(studentId, password)
                if (response.error != null) {
                    Result.failure(Exception(response.error))
                } else if (response.overall_attendance == null && response.subject_wise_attendance == null) {
                    // Check if error response is inside other fields or if the response is empty
                    Result.failure(Exception("Invalid API response format"))
                } else {
                    // Save to secure preferences
                    val jsonStr = json.encodeToString(AttendanceResponse.serializer(), response)
                    prefs.attendanceCache = jsonStr
                    prefs.studentId = studentId
                    prefs.password = password
                    prefs.lastUpdated = System.currentTimeMillis()
                    _attendance.value = response
                    Result.success(response)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun logout() {
        prefs.clearCredentials()
        _attendance.value = null
    }
}
