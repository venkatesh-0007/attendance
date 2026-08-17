package com.attendance.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.data.repository.AttendanceRepository
import com.attendance.app.theme.AttendanceTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: AttendanceRepository

    private var targetScreenState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWidgetIntent(intent)

        setContent {
            val prefs = remember { SecurePreferences(applicationContext) }
            var darkModeSetting by remember { mutableStateOf(prefs.darkMode) }
            var accentColorHex by remember { mutableStateOf(prefs.accentColor) }

            DisposableEffect(Unit) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "dark_mode" -> darkModeSetting = prefs.darkMode
                        "accent_color_hex" -> accentColorHex = prefs.accentColor
                    }
                }
                val sharedPrefs = applicationContext.getSharedPreferences("secure_attendance_prefs", Context.MODE_PRIVATE)
                sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val darkTheme = when (darkModeSetting) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            val accentColor = remember(accentColorHex) {
                accentColorHex?.let { hex ->
                    try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            AttendanceTheme(
                darkTheme = darkTheme,
                accentColor = accentColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(initialTarget = targetScreenState)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return
        val studentId = intent.getStringExtra("selected_student_id")
        val targetScreen = intent.getStringExtra("target_screen")

        if (!studentId.isNullOrBlank()) {
            repository.switchAccount(studentId)
        }
        if (!targetScreen.isNullOrBlank()) {
            targetScreenState = targetScreen
        }
    }
}
