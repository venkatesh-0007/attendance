package com.example.attendance.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.example.attendance.MainActivity
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.AttendanceResponse
import com.example.attendance.data.model.AttendanceStatus
import com.example.attendance.data.model.AttendanceWidgetState
import com.example.attendance.data.model.OverallAttendanceStatus
import com.example.attendance.worker.SyncWorker
import kotlinx.serialization.json.Json
import java.util.Locale

class AttendanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val widgetPrefs = currentState<Preferences>()
            val isRefreshing = widgetPrefs[isRefreshingKey] ?: false

            val securePrefs = remember { SecurePreferences(context) }
            val studentIdToLoad = securePrefs.studentId
            val json = remember { Json { ignoreUnknownKeys = true } }
            val cachedJson = studentIdToLoad?.let { securePrefs.getAttendanceCache(it) }

            val response: AttendanceResponse? = remember(cachedJson) {
                cachedJson?.let {
                    try {
                        json.decodeFromString<AttendanceResponse>(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val widgetState: AttendanceWidgetState? = remember(response, securePrefs.lastUpdated) {
                response?.toWidgetState(securePrefs.notificationThreshold.toDouble(), securePrefs.lastUpdated)
            }

            val darkMode = securePrefs.darkMode
            val isDark = WidgetThemeHelper.isDarkTheme(context, darkMode)

            WidgetThemeHelper.AttendanceWidgetTheme(
                context = context,
                darkModeSetting = darkMode
            ) {
                SmallWidgetContent(widgetState, isRefreshing, isDark, securePrefs)
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(
        state: AttendanceWidgetState?,
        isRefreshing: Boolean,
        isDark: Boolean,
        securePrefs: SecurePreferences
    ) {
        val surfaceBg = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
        val onSurfaceColor = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
        val onSurfaceVariantColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(surfaceBg))
            .cornerRadius(20.dp)
            .padding(14.dp)

        Column(
            modifier = rootModifier,
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            if (state == null) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Tap to login",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(onSurfaceVariantColor),
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(createTargetAction("DASHBOARD"))
                    )
                }
            } else {
                val isCritical = state.attendanceStatus == OverallAttendanceStatus.CRITICAL
                val statusColor = if (isCritical) (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)) else onSurfaceColor

                // 1. Present Attendance Percentage & Refresh Button Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier.defaultWeight().clickable(createTargetAction("DASHBOARD"))
                    ) {
                        Text(
                            text = "Attendance",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(onSurfaceVariantColor)
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", state.attendancePercentage),
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(statusColor)
                            )
                        )
                    }

                    // Refresh Button
                    Box(
                        modifier = GlanceModifier
                            .size(24.dp)
                            .clickable(actionRunCallback<AttendanceRefreshCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                color = ColorProvider(onSurfaceColor),
                                modifier = GlanceModifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = "↻",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(onSurfaceVariantColor),
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }

                // 2. Periods Can Skip Badge
                val isCanSkip = !isCritical && state.periodsCanSkip > 0
                val skipBg = if (isCanSkip) {
                    if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
                } else {
                    if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)
                }
                val skipText = if (isCanSkip) onSurfaceColor else (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626))
                val skipLabel = if (isCanSkip) "Skip: ${state.periodsCanSkip} periods" else "Attend: ${state.periodsNeedToAttend} periods"

                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(skipBg))
                        .cornerRadius(10.dp)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(createTargetAction("PREDICTION")),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = skipLabel,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(skipText)
                        )
                    )
                }

                // 3. Today's Attendance Sequence (e.g. PPPPPPP)
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(createTargetAction("TODAYS_REGISTER"))
                ) {
                    Text(
                        text = "Today",
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(onSurfaceVariantColor)
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    if (state.todayAttendanceTimeline.isEmpty()) {
                        Text(
                            text = "No classes",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(onSurfaceColor)
                            )
                        )
                    } else {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.todayAttendanceTimeline.take(8).forEachIndexed { index, status ->
                                CompactChip(status = status, isDark = isDark)
                                if (index < state.todayAttendanceTimeline.size - 1 && index < 7) {
                                    Spacer(modifier = GlanceModifier.width(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CompactChip(status: AttendanceStatus, isDark: Boolean) {
        val (bg, textColor, symbol) = when (status) {
            AttendanceStatus.PRESENT -> Triple(
                if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7),
                if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                "P"
            )
            AttendanceStatus.ABSENT -> Triple(
                if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626),
                Color.White,
                "A"
            )
            AttendanceStatus.HOLIDAY -> Triple(
                if (isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8),
                if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                "H"
            )
            AttendanceStatus.LEAVE -> Triple(
                if (isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8),
                if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B),
                "L"
            )
            AttendanceStatus.UPCOMING -> Triple(
                if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5),
                if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                "-"
            )
        }

        Box(
            modifier = GlanceModifier
                .size(16.dp)
                .background(ColorProvider(bg))
                .cornerRadius(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(textColor),
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    private fun createTargetAction(targetScreen: String): Action {
        return actionStartActivity<MainActivity>(
            actionParametersOf(targetScreenKey to targetScreen)
        )
    }

    companion object {
        val isRefreshingKey = booleanPreferencesKey("is_refreshing")
        val targetScreenKey = ActionParameters.Key<String>("target_screen")

        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(AttendanceWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[isRefreshingKey] = false
                    }
                }
                AttendanceWidget().update(context, id)
            }
        }
    }
}

class AttendanceRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[AttendanceWidget.isRefreshingKey] = true
            }
        }
        AttendanceWidget().update(context, glanceId)

        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
