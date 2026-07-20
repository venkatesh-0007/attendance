package com.example.attendance.data.local

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
        private const val KEY_NOTIFICATION_THRESHOLD = "notification_threshold"
        private const val KEY_LAST_UPDATED = "last_updated_timestamp"
        private const val KEY_ATTENDANCE_CACHE = "attendance_cache"
    }

    var studentId: String?
        get() = prefs.getString(KEY_STUDENT_ID, null)
        set(value) = prefs.edit().putString(KEY_STUDENT_ID, value).apply()

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var refreshIntervalMinutes: Int
        get() = prefs.getInt(KEY_REFRESH_INTERVAL, 180) // default to 3 hours (180 mins)
        set(value) = prefs.edit().putInt(KEY_REFRESH_INTERVAL, value).apply()

    var darkMode: String
        get() = prefs.getString(KEY_DARK_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value).apply()

    var notificationThreshold: Int
        get() = prefs.getInt(KEY_NOTIFICATION_THRESHOLD, 75) // default 75%
        set(value) = prefs.edit().putInt(KEY_NOTIFICATION_THRESHOLD, value).apply()

    var lastUpdated: Long
        get() = prefs.getLong(KEY_LAST_UPDATED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATED, value).apply()

    var attendanceCache: String?
        get() = prefs.getString(KEY_ATTENDANCE_CACHE, null)
        set(value) = prefs.edit().putString(KEY_ATTENDANCE_CACHE, value).apply()

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_STUDENT_ID)
            .remove(KEY_PASSWORD)
            .remove(KEY_ATTENDANCE_CACHE)
            .apply()
    }

    val hasCredentials: Boolean
        get() = !studentId.isNullOrBlank() && !password.isNullOrBlank()
}
