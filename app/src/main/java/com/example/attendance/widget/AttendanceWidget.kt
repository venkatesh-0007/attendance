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
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.attendance.MainActivity
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.AttendanceResponse
import com.example.attendance.worker.SyncWorker
import kotlinx.serialization.json.Json
import java.util.Locale

class AttendanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefsState = currentState<Preferences>()
            val isRefreshing = prefsState[isRefreshingKey] ?: false

            // Load data from secure prefs
            val securePrefs = remember { SecurePreferences(context) }
            val json = remember { Json { ignoreUnknownKeys = true } }
            val cachedJson = securePrefs.attendanceCache

            val data: AttendanceResponse? = remember(cachedJson) {
                if (!cachedJson.isNullOrBlank()) {
                    try {
                        json.decodeFromString<AttendanceResponse>(cachedJson)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }

            GlanceTheme {
                WidgetContent(context, data, isRefreshing)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, data: AttendanceResponse?, isRefreshing: Boolean) {
        val rootModifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity<MainActivity>())

        Column(
            modifier = rootModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Title & Refresh Button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                if (isRefreshing) {
                    Text(
                        text = "Syncing...",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.primary
                        )
                    )
                } else {
                    Text(
                        text = "↻",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        ),
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshCallback>())
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (data == null) {
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to login.",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            } else {
                val percentage = data.overall_attendance ?: 0.0
                val formattedPercent = String.format(Locale.getDefault(), "%.2f%%", percentage)
                val percentColor = if (percentage >= 75.0) GlanceTheme.colors.primary else GlanceTheme.colors.error

                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formattedPercent,
                            style = TextStyle(
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = percentColor
                            )
                        )
                        Text(
                            text = "${data.attended_classes ?: 0} / ${data.held_classes ?: 0}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                val todayStr = data.todays_attendance
                val footerText = if (todayStr.isNullOrBlank() || todayStr.equals("No Classes", true)) {
                    "Today: No Classes"
                } else {
                    "Today: $todayStr"
                }

                Text(
                    text = footerText,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        }
    }

    companion object {
        val isRefreshingKey = booleanPreferencesKey("is_refreshing")

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

class RefreshCallback : ActionCallback {
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
