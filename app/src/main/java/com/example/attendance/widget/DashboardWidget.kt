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

class DashboardWidget : GlanceAppWidget() {

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
                response?.toWidgetState(securePrefs.lastUpdated)
            }

            GlanceTheme {
                DashboardContent(widgetState, isRefreshing)
            }
        }
    }

    @Composable
    private fun DashboardContent(
        state: AttendanceWidgetState?,
        isRefreshing: Boolean
    ) {
        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(12.dp)

        Column(
            modifier = rootModifier,
            verticalAlignment = Alignment.Top
        ) {
            // Header Row: Title & Student Info (Left) | Last Updated & Fixed 24dp Refresh Box (Right)
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Attendance",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    if (state != null && state.studentName.isNotEmpty()) {
                        Text(
                            text = state.studentName,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }

                if (state != null && state.lastUpdated.isNotEmpty()) {
                    Text(
                        text = state.lastUpdated,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        modifier = GlanceModifier.padding(end = 6.dp)
                    )
                }

                // Fixed 24dp container for refresh button to prevent any layout shift
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<DashboardRefreshCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = GlanceTheme.colors.primary,
                            modifier = GlanceModifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            if (state == null) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Tap to login",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(createTargetAction("DASHBOARD"))
                    )
                }
            } else {
                // Top Middle Content Row: Percentage & Status (Left) | Skip/Attend Card (Right)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Percentage, Status Pill & Target Margin
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

                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .clickable(createTargetAction("DASHBOARD")),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f%%", state.attendancePercentage),
                                style = TextStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(statusColor)
                                )
                            )

                            Spacer(modifier = GlanceModifier.width(6.dp))

                            // Status Pill
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(statusBgColor))
                                    .cornerRadius(8.dp)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusText,
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(statusColor)
                                    )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${state.attendedClasses} / ${state.heldClasses} Classes",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GlanceTheme.colors.onSurface
                                )
                            )
                            if (state.targetMargin.isNotEmpty()) {
                                Text(
                                    text = " • ${state.targetMargin}",
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Progress Bar Indicator
                        ProgressBarTrack(percentage = state.attendancePercentage, accentColor = statusColor)
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Right Box: Skip or Attend Card
                    val isCanSkip = state.attendanceStatus != OverallAttendanceStatus.CRITICAL && state.periodsCanSkip > 0
                    val cardBg = if (isCanSkip) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    val cardTextAccent = if (isCanSkip) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val cardEmoji = if (isCanSkip) "🟢" else "🔴"
                    val cardTitle = if (isCanSkip) "Can Skip" else "Need To Attend"
                    val cardSubtitle = if (isCanSkip) "${state.periodsCanSkip} Periods" else "${state.periodsNeedToAttend} Periods"
                    val cardHint = if (isCanSkip) "Safe to miss" else "Required for 75%"

                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(cardBg))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .clickable(createTargetAction("PREDICTION")),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = cardEmoji,
                                    style = TextStyle(fontSize = 10.sp)
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                Text(
                                    text = cardTitle,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(cardTextAccent)
                                    )
                                )
                            }
                            Text(
                                text = cardSubtitle,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(cardTextAccent)
                                )
                            )
                            Text(
                                text = cardHint,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    color = ColorProvider(cardTextAccent.copy(alpha = 0.8f))
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Bottom Section: Today's Attendance Timeline
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.surfaceVariant)
                        .cornerRadius(12.dp)
                        .padding(8.dp)
                        .clickable(createTargetAction("TODAYS_REGISTER"))
                ) {
                    Text(
                        text = "Today's Attendance Timeline",
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    if (state.todayAttendanceTimeline.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📅",
                                style = TextStyle(fontSize = 12.sp)
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Column {
                                Text(
                                    text = "No Classes Scheduled Today",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GlanceTheme.colors.onSurface
                                    )
                                )
                                Text(
                                    text = "Tap to view register grid & history",
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    } else {
                        // Dynamic Period-Numbered Chip Row
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.todayAttendanceTimeline.forEachIndexed { index, status ->
                                NumberedTimelineChip(periodNum = index + 1, status = status)
                                if (index < state.todayAttendanceTimeline.size - 1) {
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ProgressBarTrack(percentage: Double, accentColor: Color) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ColorProvider(Color(0xFFE0E0E0)))
                .cornerRadius(2.dp)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxHeight()
                    .defaultWeight()
                    .background(ColorProvider(accentColor))
                    .cornerRadius(2.dp)
            ) {}
        }
    }

    @Composable
    private fun NumberedTimelineChip(periodNum: Int, status: AttendanceStatus) {
        val (bg, textColor, symbol) = when (status) {
            AttendanceStatus.PRESENT -> Triple(Color(0xFF2E7D32), Color.White, "P")
            AttendanceStatus.ABSENT -> Triple(Color(0xFFC62828), Color.White, "A")
            AttendanceStatus.HOLIDAY -> Triple(Color(0xFF1565C0), Color.White, "H")
            AttendanceStatus.LEAVE -> Triple(Color(0xFF6A1B9A), Color.White, "L")
            AttendanceStatus.UPCOMING -> Triple(Color(0xFFE0E0E0), Color(0xFF616161), "-")
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "P$periodNum",
                style = TextStyle(
                    fontSize = 8.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
            Spacer(modifier = GlanceModifier.height(1.dp))
            Box(
                modifier = GlanceModifier
                    .size(22.dp)
                    .background(ColorProvider(bg))
                    .cornerRadius(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(textColor),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }

    private fun createTargetAction(targetScreen: String): Action {
        return actionStartActivity<MainActivity>(
            actionParametersOf(targetScreenKey to targetScreen)
        )
    }

    companion object {
        val isRefreshingKey = booleanPreferencesKey("dashboard_is_refreshing")
        val targetScreenKey = ActionParameters.Key<String>("target_screen")

        suspend fun updateAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(DashboardWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[isRefreshingKey] = false
                    }
                }
                DashboardWidget().update(context, id)
            }
        }
    }
}

class DashboardRefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[DashboardWidget.isRefreshingKey] = true
            }
        }
        DashboardWidget().update(context, glanceId)

        val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
