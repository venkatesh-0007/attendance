package com.attendance.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.ui.login.LoginScreen
import com.attendance.app.ui.dashboard.DashboardScreen
import com.attendance.app.ui.attendance.AttendanceScreen
import com.attendance.app.ui.attendance.SubjectDetailScreen
import com.attendance.app.ui.attendance.AttendanceTableScreen
import com.attendance.app.ui.timetable.TimetableScreen
import com.attendance.app.ui.settings.SettingsScreen
import com.attendance.app.ui.calendar.AttendanceCalendarScreen
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

@Composable
fun MainNavigation(initialTarget: String? = null) {
    val context = LocalContext.current
    val prefs = remember { SecurePreferences(context) }
    val isLoggedIn = prefs.hasCredentials

    val startDestination = if (isLoggedIn) Dashboard else Login

    val backStack = rememberNavBackStack(startDestination)

    fun navigateTo(destination: androidx.navigation3.runtime.NavKey) {
        if (backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    fun popBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        popBack()
    }

    LaunchedEffect(initialTarget, isLoggedIn) {
        if (isLoggedIn && initialTarget != null) {
            when (initialTarget) {
                "TODAYS_REGISTER" -> navigateTo(AttendanceTable)
                "PREDICTION" -> navigateTo(Attendance)
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
        onBack = { popBack() },
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
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
                    onNavigateToAttendance = { navigateTo(Attendance) },
                    onNavigateToTimetable = { navigateTo(Timetable) },
                    onNavigateToSettings = { navigateTo(Settings) },
                    onNavigateToCalendar = { navigateTo(Calendar) },
                    onNavigateToTable = { navigateTo(AttendanceTable) }
                )
            }
            entry<Attendance> {
                AttendanceScreen(
                    onNavigateToSubjectDetails = { name -> navigateTo(SubjectDetails(name)) },
                    onNavigateToTable = { navigateTo(AttendanceTable) },
                    onBack = { popBack() }
                )
            }
            entry<SubjectDetails> { key ->
                SubjectDetailScreen(
                    subjectName = key.subjectName,
                    onBack = { popBack() }
                )
            }
            entry<AttendanceTable> {
                AttendanceTableScreen(
                    onBack = { popBack() }
                )
            }
            entry<Timetable> {
                TimetableScreen(
                    onBack = { popBack() }
                )
            }
            entry<Settings> {
                SettingsScreen(
                    onAddAccount = { navigateTo(Login) },
                    onLogout = {
                        while (backStack.size > 0) {
                            backStack.removeLastOrNull()
                        }
                        backStack.add(Login)
                    },
                    onBack = { popBack() }
                )
            }
            entry<Calendar> {
                AttendanceCalendarScreen(
                    onBack = { popBack() }
                )
            }
        }
    )
}
