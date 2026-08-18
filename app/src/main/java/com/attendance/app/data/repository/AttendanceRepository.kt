package com.attendance.app.data.repository

import com.attendance.app.data.api.AttendanceApi
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.data.model.AttendanceResponse
import com.attendance.app.data.model.UserAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

class AttendanceRepository(
    private val api: AttendanceApi,
    private val prefs: SecurePreferences,
    private val json: Json,
    private val context: android.content.Context? = null
) {
    private val _attendance = MutableStateFlow<AttendanceResponse?>(null)
    val attendance: StateFlow<AttendanceResponse?> = _attendance.asStateFlow()

    private val _lastUpdated = MutableStateFlow<Long>(0L)
    val lastUpdated: StateFlow<Long> = _lastUpdated.asStateFlow()

    private val _currentAccount = MutableStateFlow<UserAccount?>(null)
    val currentAccount: StateFlow<UserAccount?> = _currentAccount.asStateFlow()

    init {
        loadCachedData()
    }

    private fun updateStateFlows(studentId: String?) {
        val id = studentId ?: prefs.studentId
        if (!id.isNullOrBlank()) {
            _lastUpdated.value = prefs.getLastUpdated(id)
            _currentAccount.value = getSavedAccounts().find { it.studentId == id }
        } else {
            _lastUpdated.value = 0L
            _currentAccount.value = null
        }
    }

    private fun loadCachedData() {
        val studentId = prefs.studentId ?: run {
            updateStateFlows(null)
            return
        }
        updateStateFlows(studentId)
        val cachedJson = prefs.getAttendanceCache(studentId)
        if (!cachedJson.isNullOrBlank()) {
            try {
                _attendance.value = json.decodeFromString<AttendanceResponse>(cachedJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getSavedAccounts(): List<UserAccount> {
        val jsonStr = prefs.accountsJson ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(UserAccount.serializer()), jsonStr)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveAccounts(accounts: List<UserAccount>) {
        val jsonStr = json.encodeToString(ListSerializer(UserAccount.serializer()), accounts)
        prefs.accountsJson = jsonStr
        updateStateFlows(prefs.studentId)
    }

    fun switchAccount(studentId: String) {
        val accounts = getSavedAccounts()
        val account = accounts.find { it.studentId == studentId } ?: return
        
        prefs.studentId = account.studentId
        prefs.password = account.password
        _attendance.value = null // Clear current state to force reload from new cache
        loadCachedData()
        context?.let { ctx ->
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                com.attendance.app.widget.WidgetUpdater.updateAll(ctx)
            }
        }
    }

    fun removeAccount(studentId: String) {
        val accounts = getSavedAccounts().toMutableList()
        accounts.removeAll { it.studentId == studentId }
        saveAccounts(accounts)
        prefs.removeAccountData(studentId)
        
        if (prefs.studentId == studentId) {
            logout()
        } else {
            updateStateFlows(prefs.studentId)
        }
    }

    fun updateAccountCustomName(studentId: String, customName: String) {
        val accounts = getSavedAccounts().toMutableList()
        val idx = accounts.indexOfFirst { it.studentId == studentId }
        if (idx != -1) {
            val account = accounts[idx]
            accounts[idx] = account.copy(customName = customName.ifBlank { null })
            saveAccounts(accounts)
        }
    }

    suspend fun fetchAttendance(studentId: String, password: String): Result<AttendanceResponse> =
        withContext(Dispatchers.IO) {
            try {
                val rawResponse = api.getAttendance(studentId, password)
                
                val actualJson = try {
                    json.decodeFromString<String>(rawResponse)
                } catch (_: Exception) {
                    rawResponse
                }

                val response = json.decodeFromString<AttendanceResponse>(actualJson)

                if (response.error != null) {
                    Result.failure(Exception(response.error))
                } else if (response.totalInfo == null && response.subjectwiseSummary == null) {
                    Result.failure(Exception("Invalid API response format"))
                } else {
                    // Update saved accounts list
                    val accounts = getSavedAccounts().toMutableList()
                    val existingIdx = accounts.indexOfFirst { it.studentId == studentId }
                    val existingCustomName = if (existingIdx != -1) accounts[existingIdx].customName else null
                    val name = response.studentName ?: response.rollNumber
                    val newAccount = UserAccount(
                        studentId = studentId,
                        password = password,
                        studentName = name,
                        customName = existingCustomName
                    )
                    if (existingIdx != -1) {
                        accounts[existingIdx] = newAccount
                    } else {
                        accounts.add(newAccount)
                    }
                    saveAccounts(accounts)

                    // Save to secure preferences
                    val jsonStr = json.encodeToString(AttendanceResponse.serializer(), response)
                    val now = System.currentTimeMillis()
                    prefs.setAttendanceCache(studentId, jsonStr)
                    prefs.studentId = studentId
                    prefs.password = password
                    prefs.setLastUpdated(studentId, now)
                    _attendance.value = response
                    updateStateFlows(studentId)

                    context?.let { ctx ->
                        com.attendance.app.widget.WidgetUpdater.updateAll(ctx)
                    }

                    Result.success(response)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun logout() {
        prefs.clearCredentials()
        _attendance.value = null
        updateStateFlows(null)
    }

    fun clearCache() {
        val studentId = prefs.studentId ?: return
        prefs.removeAccountData(studentId)
        _attendance.value = null
    }
}
