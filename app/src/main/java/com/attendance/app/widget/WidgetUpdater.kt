package com.attendance.app.widget

import android.content.Context

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        try {
            DashboardWidget.updateAll(context)
            AttendanceWidget.updateAll(context)
            BadgeWidget.updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
