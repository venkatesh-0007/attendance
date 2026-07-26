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
import com.example.attendance.data.model.AttendanceWidgetState
import com.example.attendance.data.model.OverallAttendanceStatus
import com.example.attendance.worker.SyncWorker
import kotlinx.serialization.json.Json
import java.util.Locale

class BadgeWidget : GlanceAppWidget() {

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
                BadgeContent(widgetState, isRefreshing)
            }
        }
    }

    @Composable
    private fun BadgeContent(
        state: AttendanceWidgetState?,
        isRefreshing: Boolean
    ) {
        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)

        Row(
            modifier = rootModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state == null) {
                Text(
                    text = if (isRefreshing) "Refreshing..." else "Tap to login",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    ),
                    modifier = GlanceModifier.defaultWeight().clickable(createTargetAction("DASHBOARD"))
                )
            } else {
                val statusColor = when (state.attendanceStatus) {
                    OverallAttendanceStatus.SAFE -> Color(0xFF2E7D32)
                    OverallAttendanceStatus.WARNING -> Color(0xFFEF6C00)
                    OverallAttendanceStatus.CRITICAL -> Color(0xFFC62828)
                }

                Row(
                    modifier = GlanceModifier.defaultWeight().clickable(createTargetAction("DASHBOARD")),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", state.attendancePercentage),
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(statusColor)
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    Text(
                        text = "(${state.attendedClasses}/${state.heldClasses})",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

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
                        textAlign = TextAlign.End
                    ),
                    modifier = GlanceModifier.clickable(actionRunCallback<BadgeRefreshCallback>())
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
        val isRefreshingKey = booleanPreferencesKey("badge_is_refreshing")
        val targetScreenKey = ActionParameters.Key<String>("target_screen")

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
