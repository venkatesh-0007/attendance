package com.attendance.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.*
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.GlanceTheme
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.attendance.app.MainActivity
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.data.model.AttendanceResponse
import com.attendance.app.data.model.AttendanceStatus
import com.attendance.app.data.model.AttendanceWidgetState
import com.attendance.app.data.model.OverallAttendanceStatus
import com.attendance.app.worker.SyncWorker
import kotlinx.serialization.json.Json
import java.util.Locale

class BadgeWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val widgetPrefs = currentState<Preferences>()
            val isRefreshing = widgetPrefs[isRefreshingKey] ?: false
            val securePrefs = remember { SecurePreferences(context) }
            val selectedStudentId = widgetPrefs[SELECTED_STUDENT_ID_KEY] ?: securePrefs.studentId
            val json = remember { Json { ignoreUnknownKeys = true } }
            val cachedJson = selectedStudentId?.let { securePrefs.getAttendanceCache(it) }

            val savedAccountsJson = securePrefs.accountsJson
            val savedAccount = remember(savedAccountsJson, selectedStudentId) {
                if (!savedAccountsJson.isNullOrBlank() && selectedStudentId != null) {
                    try {
                        val list = json.decodeFromString<List<com.attendance.app.data.model.UserAccount>>(savedAccountsJson)
                        list.find { it.studentId == selectedStudentId }
                    } catch (_: Exception) { null }
                } else null
            }

            val displayName = savedAccount?.displayName

            val response: AttendanceResponse? = remember(cachedJson) {
                cachedJson?.let {
                    try {
                        json.decodeFromString<AttendanceResponse>(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val widgetState: AttendanceWidgetState? = remember(response, securePrefs.lastUpdated, displayName) {
                response?.toWidgetState(
                    targetThreshold = securePrefs.notificationThreshold.toDouble(),
                    lastUpdatedMillis = securePrefs.lastUpdated,
                    customName = displayName
                )
            }

            val darkMode = securePrefs.darkMode
            val isDark = WidgetThemeHelper.isDarkTheme(context, darkMode)

            WidgetThemeHelper.AttendanceWidgetTheme(
                context = context,
                darkModeSetting = darkMode
            ) {
                BadgeContent(widgetState, isRefreshing, isDark, selectedStudentId)
            }
        }
    }

    @Composable
    private fun BadgeContent(
        state: AttendanceWidgetState?,
        isRefreshing: Boolean,
        isDark: Boolean,
        studentId: String?
    ) {
        val surfaceBg = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
        val onSurfaceColor = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
        val onSurfaceVariantColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

        val size = LocalSize.current
        val isWide = size.width >= 200.dp

        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(surfaceBg))
            .cornerRadius(18.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)

        Row(
            modifier = rootModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == null) {
                Text(
                    text = if (isRefreshing) "Refreshing..." else "Tap to login",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(onSurfaceVariantColor)
                    ),
                    modifier = GlanceModifier.defaultWeight().clickable(createTargetAction("DASHBOARD", studentId))
                )
            } else {
                val isCritical = state.attendanceStatus == OverallAttendanceStatus.CRITICAL
                val statusColor = if (isCritical) (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)) else onSurfaceColor
                val statusBgColor = if (isCritical) (if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)) else (if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))

                // Left: Pill badge showing percentage
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(statusBgColor))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable(createTargetAction("DASHBOARD", studentId)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", state.attendancePercentage),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(statusColor)
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                Column(
                    modifier = GlanceModifier.defaultWeight().clickable(createTargetAction("DASHBOARD", studentId)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nameStr = if (state.studentName.isNotBlank()) state.studentName else "Attendance"

                    val skipText = when {
                        state.periodsCanSkip > 0 -> if (isWide) "${state.periodsCanSkip} periods skip" else "${state.periodsCanSkip} skip"
                        state.periodsNeedToAttend > 0 -> "Need ${state.periodsNeedToAttend}"
                        else -> "On target"
                    }

                    val skipColor = when {
                        state.periodsCanSkip > 0 -> if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                        state.periodsNeedToAttend > 0 -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                        else -> onSurfaceVariantColor
                    }

                    // Line 1: Name + Skip info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        Text(
                            text = nameStr,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(onSurfaceColor)
                            ),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        Text(
                            text = skipText,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(skipColor)
                            ),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(1.dp))

                    // Line 2: Today's Sequence
                    val todaySeq = if (state.todayAttendanceTimeline.isNotEmpty()) {
                        state.todayAttendanceTimeline.joinToString("") { status ->
                            when (status) {
                                AttendanceStatus.PRESENT -> "P"
                                AttendanceStatus.ABSENT -> "A"
                                AttendanceStatus.HOLIDAY -> "H"
                                AttendanceStatus.LEAVE -> "L"
                                AttendanceStatus.UPCOMING -> "-"
                            }
                        }
                    } else "No classes"

                    val todayLabel = if (isWide) "Today's Timeline: $todaySeq" else "Today: $todaySeq"

                    Text(
                        text = todayLabel,
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(onSurfaceVariantColor)
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(4.dp))

            // Refresh Button on Right
            Box(
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionRunCallback<BadgeRefreshCallback>()),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = ColorProvider(onSurfaceColor),
                        modifier = GlanceModifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(onSurfaceColor),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }

    private fun createTargetAction(targetScreen: String, studentId: String? = null): Action {
        val params = mutableListOf<ActionParameters.Pair<*>>(
            targetScreenKey to targetScreen
        )
        if (!studentId.isNullOrBlank()) {
            params.add(studentIdKey to studentId)
        }
        return actionStartActivity<MainActivity>(
            actionParametersOf(*params.toTypedArray())
        )
    }

    companion object {
        val isRefreshingKey = booleanPreferencesKey("badge_is_refreshing")
        val targetScreenKey = ActionParameters.Key<String>("target_screen")
        val studentIdKey = ActionParameters.Key<String>("selected_student_id")

        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(BadgeWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[isRefreshingKey] = false
                    }
                }
                BadgeWidget().update(context, id)
            }
        }
    }
}

class BadgeRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[BadgeWidget.isRefreshingKey] = true
            }
        }
        BadgeWidget().update(context, glanceId)

        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
