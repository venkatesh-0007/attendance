package com.example.attendance.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.attendance.MainActivity
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.AttendanceResponse
import com.example.attendance.worker.SyncWorker
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

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

            val data: AttendanceResponse? = remember(cachedJson) {
                cachedJson?.let {
                    try {
                        json.decodeFromString<AttendanceResponse>(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            GlanceTheme {
                DashboardContent(data, isRefreshing)
            }
        }
    }

    @Composable
    private fun DashboardContent(
        data: AttendanceResponse?,
        isRefreshing: Boolean
    ) {
        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(14.dp)

        Column(
            modifier = rootModifier,
            verticalAlignment = Alignment.Top
        ) {
            // Header Row
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data?.student_name ?: "Attendance Dashboard",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                if (isRefreshing) {
                    CircularProgressIndicator(
                        color = GlanceTheme.colors.primary,
                        modifier = GlanceModifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary,
                            textAlign = TextAlign.End
                        ),
                        modifier = GlanceModifier.clickable(actionRunCallback<DashboardRefreshCallback>())
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (data == null) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Offline / Tap to login",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                    )
                }
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Stats & Gauge Summary
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(actionStartActivity<MainActivity>()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val percentage = data.overallPercentage
                        val isSafe = percentage >= 75.0
                        val percentColor = if (isSafe) GlanceTheme.colors.primary else GlanceTheme.colors.error

                        Text(
                            text = String.format(Locale.getDefault(), "%.2f%%", percentage),
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = percentColor,
                                textAlign = TextAlign.Center
                            )
                        )

                        Text(
                            text = if (isSafe) "Good Standing" else "Low Attendance",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = percentColor,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        Text(
                            text = "${data.total_info?.total_attended ?: 0} / ${data.total_info?.total_held ?: 0} Attended",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = GlanceTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Right Column: Subject Breakdown & Today Badges
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .clickable(actionStartActivity<MainActivity>()),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Subjects",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.primary
                            )
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        val topSubjects = data.subjectwise_summary?.take(3) ?: emptyList()
                        topSubjects.forEach { sub ->
                            Row(
                                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sub.subject_name,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = GlanceTheme.colors.onSurface
                                    ),
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Text(
                                    text = sub.percentage,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sub.percentageDouble >= 75.0) GlanceTheme.colors.primary else GlanceTheme.colors.error
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        val todayDate = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
                        val statusStr = data.getTodayStatusString(todayDate)
                        val todayText = if (statusStr.isNotBlank()) {
                            "Today: $statusStr"
                        } else {
                            "Today: No Classes"
                        }

                        Text(
                            text = todayText,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        val isRefreshingKey = booleanPreferencesKey("dashboard_is_refreshing")

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
