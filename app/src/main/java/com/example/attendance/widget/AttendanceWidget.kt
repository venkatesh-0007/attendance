package com.example.attendance.widget

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

            GlanceTheme {
                SmallWidgetContent(widgetState, isRefreshing)
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(
        state: AttendanceWidgetState?,
        isRefreshing: Boolean
    ) {
        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(18.dp)
            .padding(10.dp)

        Column(
            modifier = rootModifier,
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header Row: Title & Fixed 24dp Refresh Box
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                // Fixed 24dp container for zero layout shift
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<AttendanceRefreshCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = GlanceTheme.colors.primary,
                            modifier = GlanceModifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            if (state == null) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Tap to login",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(createTargetAction("DASHBOARD"))
                    )
                }
            } else {
                val statusColor = when (state.attendanceStatus) {
                    OverallAttendanceStatus.SAFE -> Color(0xFF2E7D32)
                    OverallAttendanceStatus.WARNING -> Color(0xFFEF6C00)
                    OverallAttendanceStatus.CRITICAL -> Color(0xFFC62828)
                }

                val statusBgColor = when (state.attendanceStatus) {
                    OverallAttendanceStatus.SAFE -> Color(0xFFE8F5E9)
                    OverallAttendanceStatus.WARNING -> Color(0xFFFFF3E0)
                    OverallAttendanceStatus.CRITICAL -> Color(0xFFFFEBEE)
                }

                val statusText = when (state.attendanceStatus) {
                    OverallAttendanceStatus.SAFE -> "SAFE"
                    OverallAttendanceStatus.WARNING -> "WARNING"
                    OverallAttendanceStatus.CRITICAL -> "CRITICAL"
                }

                // Row 1: Percentage & Status Pill
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(createTargetAction("DASHBOARD")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", state.attendancePercentage),
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(statusColor)
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(statusBgColor))
                            .cornerRadius(6.dp)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusText,
                            style = TextStyle(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(statusColor)
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Row 2: Skip/Attend Card
                val isCanSkip = state.attendanceStatus != OverallAttendanceStatus.CRITICAL && state.periodsCanSkip > 0
                val cardBg = if (isCanSkip) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                val cardTextAccent = if (isCanSkip) Color(0xFF2E7D32) else Color(0xFFC62828)
                val cardLabel = if (isCanSkip) "🟢 Skip ${state.periodsCanSkip}" else "🔴 Attend ${state.periodsNeedToAttend}"

                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(cardBg))
                        .cornerRadius(8.dp)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .clickable(createTargetAction("PREDICTION")),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cardLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(cardTextAccent)
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // Row 3: Today Timeline Chips
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(createTargetAction("TODAYS_REGISTER"))
                ) {
                    if (state.todayAttendanceTimeline.isEmpty()) {
                        Text(
                            text = "📅 No classes today",
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    } else {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.todayAttendanceTimeline.take(8).forEachIndexed { index, status ->
                                CompactChip(status = status)
                                if (index < state.todayAttendanceTimeline.size - 1 && index < 7) {
                                    Spacer(modifier = GlanceModifier.width(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun CompactChip(status: AttendanceStatus) {
        val (bg, textColor, symbol) = when (status) {
            AttendanceStatus.PRESENT -> Triple(Color(0xFF2E7D32), Color.White, "P")
            AttendanceStatus.ABSENT -> Triple(Color(0xFFC62828), Color.White, "A")
            AttendanceStatus.HOLIDAY -> Triple(Color(0xFF1565C0), Color.White, "H")
            AttendanceStatus.LEAVE -> Triple(Color(0xFF6A1B9A), Color.White, "L")
            AttendanceStatus.UPCOMING -> Triple(Color(0xFFE0E0E0), Color(0xFF616161), "-")
        }

        Box(
            modifier = GlanceModifier
                .size(18.dp)
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
