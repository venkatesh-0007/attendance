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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private val SpatialEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

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
            (slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * 0.95f).toInt() },
                animationSpec = tween(320, easing = SpatialEasing)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(320, easing = SpatialEasing)
            ) + fadeIn(animationSpec = tween(280, easing = SpatialEasing))) togetherWith
            (slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.22f).toInt() },
                animationSpec = tween(320, easing = SpatialEasing)
            ) + scaleOut(
                targetScale = 0.95f,
                animationSpec = tween(320, easing = SpatialEasing)
            ) + fadeOut(animationSpec = tween(240, easing = SpatialEasing)))
        },
        popTransitionSpec = {
            (slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.22f).toInt() },
                animationSpec = tween(320, easing = SpatialEasing)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(320, easing = SpatialEasing)
            ) + fadeIn(animationSpec = tween(280, easing = SpatialEasing))) togetherWith
            (slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(320, easing = SpatialEasing)
            ) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(320, easing = SpatialEasing)
            ) + fadeOut(animationSpec = tween(260, easing = SpatialEasing)))
        },
        predictivePopTransitionSpec = { _ ->
            (slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.22f).toInt() },
                animationSpec = tween(320, easing = SpatialEasing)
            ) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(320, easing = SpatialEasing)
            ) + fadeIn(animationSpec = tween(280, easing = SpatialEasing))) togetherWith
            (slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(320, easing = SpatialEasing)
            ) + scaleOut(
                targetScale = 0.96f,
                animationSpec = tween(320, easing = SpatialEasing)
            ) + fadeOut(animationSpec = tween(260, easing = SpatialEasing)))
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
