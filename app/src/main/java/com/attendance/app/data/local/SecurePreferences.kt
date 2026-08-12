package com.attendance.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.EncryptedSharedPreferences

class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_attendance_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_STUDENT_ID = "student_id"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_minutes"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_ACCENT_COLOR = "accent_color_hex"
        private const val KEY_NOTIFICATION_THRESHOLD = "notification_threshold"
        private const val KEY_LAST_UPDATED = "last_updated_timestamp"
        private const val KEY_ATTENDANCE_CACHE_PREFIX = "attendance_cache_"
        private const val KEY_ACCOUNTS_JSON = "accounts_json"
        private const val KEY_AUTO_SYNC_ACTIVE_HOURS_ONLY = "auto_sync_active_hours_only"
        private const val KEY_ACTIVE_START_HOUR = "active_start_hour"
        private const val KEY_ACTIVE_END_HOUR = "active_end_hour"
    }

    var studentId: String?
        get() = prefs.getString(KEY_STUDENT_ID, null)
        set(value) = prefs.edit().putString(KEY_STUDENT_ID, value).apply()

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var refreshIntervalMinutes: Int
        get() = prefs.getInt(KEY_REFRESH_INTERVAL, 180)
        set(value) = prefs.edit().putInt(KEY_REFRESH_INTERVAL, value).apply()

    var darkMode: String
        get() = prefs.getString(KEY_DARK_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value).apply()

    var accentColor: String?
        get() = prefs.getString(KEY_ACCENT_COLOR, null)
        set(value) = prefs.edit().putString(KEY_ACCENT_COLOR, value).apply()

    var notificationThreshold: Int
        get() = prefs.getInt(KEY_NOTIFICATION_THRESHOLD, 75)
        set(value) = prefs.edit().putInt(KEY_NOTIFICATION_THRESHOLD, value).apply()

    var lastUpdated: Long
        get() = prefs.getLong(KEY_LAST_UPDATED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATED, value).apply()

    var accountsJson: String?
        get() = prefs.getString(KEY_ACCOUNTS_JSON, null)
        set(value) = prefs.edit().putString(KEY_ACCOUNTS_JSON, value).apply()

    var autoSyncActiveHoursOnly: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC_ACTIVE_HOURS_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC_ACTIVE_HOURS_ONLY, value).apply()

    var activeStartHour: Int
        get() = prefs.getInt(KEY_ACTIVE_START_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_START_HOUR, value).apply()

    var activeEndHour: Int
        get() = prefs.getInt(KEY_ACTIVE_END_HOUR, 16)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_END_HOUR, value).apply()

    fun isCurrentlyInActiveHours(): Boolean {
        if (!autoSyncActiveHoursOnly) return true
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val start = activeStartHour
        val end = activeEndHour

        return if (start <= end) {
            currentHour in start until end
        } else {
            currentHour >= start || currentHour < end
        }
    }

    fun getAttendanceCache(id: String): String? {
        return prefs.getString(KEY_ATTENDANCE_CACHE_PREFIX + id, null)
    }

    fun setAttendanceCache(id: String, json: String) {
        prefs.edit().putString(KEY_ATTENDANCE_CACHE_PREFIX + id, json).apply()
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_STUDENT_ID)
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun removeAccountData(id: String) {
        prefs.edit().remove(KEY_ATTENDANCE_CACHE_PREFIX + id).apply()
    }

    val hasCredentials: Boolean
        get() = !studentId.isNullOrBlank() && !password.isNullOrBlank()
}
