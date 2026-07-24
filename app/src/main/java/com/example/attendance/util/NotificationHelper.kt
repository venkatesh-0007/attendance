package com.example.attendance.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.attendance.MainActivity
import java.util.Locale

object NotificationHelper {

    private const val CRITICAL_CHANNEL_ID = "attendance_critical_alerts"
    private const val CRITICAL_CHANNEL_NAME = "Critical Attendance Alerts"

    private const val GENERAL_CHANNEL_ID = "attendance_sync_updates"
    private const val GENERAL_CHANNEL_NAME = "Sync Status Updates"

    private const val NOTIFICATION_ID = 1001

    fun showLowAttendanceNotification(context: Context, percentage: Double, threshold: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CRITICAL_CHANNEL_ID,
                CRITICAL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when attendance falls below the threshold"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap action
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedPercentage = String.format(Locale.getDefault(), "%.2f", percentage)
        val title = "Critical Attendance Alert!"
        val content = "Your overall attendance is $formattedPercentage%, which is below your target of $threshold%."

        val notification = NotificationCompat.Builder(context, CRITICAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Built-in fallback icon
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showSyncUpdateNotification(context: Context, status: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                GENERAL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Updates regarding background synchronization cycles"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap action
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Attendance Synchronized")
            .setContentText(status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }
}
