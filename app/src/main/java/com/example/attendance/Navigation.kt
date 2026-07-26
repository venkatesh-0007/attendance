package com.example.attendance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.ui.login.LoginScreen
import com.example.attendance.ui.dashboard.DashboardScreen
import com.example.attendance.ui.attendance.AttendanceScreen
import com.example.attendance.ui.attendance.SubjectDetailScreen
import com.example.attendance.ui.attendance.AttendanceTableScreen
import com.example.attendance.ui.timetable.TimetableScreen
import com.example.attendance.ui.settings.SettingsScreen
import com.example.attendance.ui.calendar.AttendanceCalendarScreen

@Composable
fun MainNavigation(initialTarget: String? = null) {
    val context = LocalContext.current
    val prefs = remember { SecurePreferences(context) }
    val isLoggedIn = prefs.hasCredentials

    val startDestination = if (isLoggedIn) Dashboard else Login

    val backStack = rememberNavBackStack(startDestination)

    LaunchedEffect(initialTarget, isLoggedIn) {
        if (isLoggedIn && initialTarget != null) {
            when (initialTarget) {
                "TODAYS_REGISTER" -> {
                    if (!backStack.contains(AttendanceTable)) {
                        backStack.add(AttendanceTable)
                    }
                }
                "PREDICTION" -> {
                    if (!backStack.contains(Attendance)) {
                        backStack.add(Attendance)
                    }
                }
                "DASHBOARD" -> {
                    while (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                }
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<Login> {
                LoginScreen(
                    onLoginSuccess = {
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Dashboard)
                    }
                )
            }
            entry<Dashboard> {
                DashboardScreen(
                    onNavigateToAttendance = { backStack.add(Attendance) },
                    onNavigateToTimetable = { backStack.add(Timetable) },
                    onNavigateToSettings = { backStack.add(Settings) },
                    onNavigateToCalendar = { backStack.add(Calendar) },
                    onNavigateToTable = { backStack.add(AttendanceTable) }
                )
            }
            entry<Attendance> {
                AttendanceScreen(
                    onNavigateToSubjectDetails = { name -> backStack.add(SubjectDetails(name)) },
                    onNavigateToTable = { backStack.add(AttendanceTable) },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<SubjectDetails> { key ->
                SubjectDetailScreen(
                    subjectName = key.subjectName,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<AttendanceTable> {
                AttendanceTableScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<Timetable> {
                TimetableScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<Settings> {
                SettingsScreen(
                    onAddAccount = { backStack.add(Login) },
                    onLogout = {
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<Calendar> {
                AttendanceCalendarScreen(
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        }
    )
}
