package com.attendance.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.attendance.app.worker.SyncWorker

object WidgetRefreshHelper {

    suspend fun triggerRefresh(context: Context) {
        val manager = GlanceAppWidgetManager(context)

        // Set refreshing state on all BadgeWidget instances
        val badgeIds = manager.getGlanceIds(BadgeWidget::class.java)
        badgeIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[BadgeWidget.isRefreshingKey] = true
                }
            }
            BadgeWidget().update(context, id)
        }

        // Set refreshing state on all DashboardWidget instances
        val dashboardIds = manager.getGlanceIds(DashboardWidget::class.java)
        dashboardIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[DashboardWidget.isRefreshingKey] = true
                }
            }
            DashboardWidget().update(context, id)
        }

        // Set refreshing state on all AttendanceWidget instances
        val attendanceIds = manager.getGlanceIds(AttendanceWidget::class.java)
        attendanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[AttendanceWidget.isRefreshingKey] = true
                }
            }
            AttendanceWidget().update(context, id)
        }

        // Enqueue manual sync work request with FORCE_SYNC flag
        val inputData = Data.Builder()
            .putBoolean("FORCE_SYNC", true)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
