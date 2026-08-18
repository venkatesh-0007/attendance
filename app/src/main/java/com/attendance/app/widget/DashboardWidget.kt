package com.attendance.app.widget

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
import com.attendance.app.MainActivity
import com.attendance.app.data.local.SecurePreferences
import com.attendance.app.data.model.AttendanceResponse
import com.attendance.app.data.model.AttendanceStatus
import com.attendance.app.data.model.AttendanceWidgetState
import com.attendance.app.data.model.OverallAttendanceStatus
import com.attendance.app.worker.SyncWorker
import kotlinx.serialization.json.Json
import java.util.Locale

class DashboardWidget : GlanceAppWidget() {

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
                DashboardContent(widgetState, isRefreshing, isDark, securePrefs, selectedStudentId)
            }
        }
    }

    @Composable
    private fun DashboardContent(
        state: AttendanceWidgetState?,
        isRefreshing: Boolean,
        isDark: Boolean,
        securePrefs: SecurePreferences,
        studentId: String?
    ) {
        val surfaceBg = if (isDark) Color(0xFF09090B) else Color(0xFFFFFFFF)
        val cardBg = if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5)
        val onSurfaceColor = if (isDark) Color(0xFFFAFAFA) else Color(0xFF09090B)
        val onSurfaceVariantColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

        val rootModifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ColorProvider(surfaceBg))
            .cornerRadius(24.dp)
            .padding(14.dp)

        Column(
            modifier = rootModifier,
            verticalAlignment = Alignment.Top
        ) {
            // Header Row: "Dashboard" and Active User | Last Sync & Refresh Button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    val mainTitle = if (state != null && state.studentName.isNotBlank()) state.studentName else "Dashboard"
                    Text(
                        text = mainTitle,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(onSurfaceColor)
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "Attendance Overview",
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(onSurfaceVariantColor)
                        )
                    )
                }

                if (state != null && state.lastUpdated.isNotEmpty()) {
                    Text(
                        text = state.lastUpdated,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(onSurfaceVariantColor)
                        ),
                        modifier = GlanceModifier.padding(end = 6.dp)
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .clickable(actionRunCallback<DashboardRefreshCallback>()),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = ColorProvider(onSurfaceColor),
                            modifier = GlanceModifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "↻",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(onSurfaceColor),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (state == null) {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Tap to login",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(onSurfaceVariantColor),
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier.clickable(createTargetAction("DASHBOARD", studentId))
                    )
                }
            } else {
                val isCritical = state.attendanceStatus == OverallAttendanceStatus.CRITICAL
                val statusColor = if (isCritical) (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)) else onSurfaceColor
                val statusBgColor = if (isCritical) (if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)) else (if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))

                val statusText = when (state.attendanceStatus) {
                    OverallAttendanceStatus.SAFE -> "SAFE"
                    OverallAttendanceStatus.WARNING -> "WARNING"
                    OverallAttendanceStatus.CRITICAL -> "CRITICAL"
                }

                // 1. OVERALL PERCENTAGE GAUGE CARD (Gauge left, metrics middle, prediction box far right)
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(cardBg))
                        .cornerRadius(20.dp)
                        .padding(12.dp)
                        .clickable(createTargetAction("DASHBOARD", studentId)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Circular Gauge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = GlanceModifier.size(76.dp)
                        ) {
                            val threshold = securePrefs.notificationThreshold
                            val gaugeBitmap = remember(state.attendancePercentage, isDark) {
                                createCircularGaugeBitmap(
                                    percentage = state.attendancePercentage,
                                    threshold = threshold,
                                    isDark = isDark
                                )
                            }
                            Image(
                                provider = ImageProvider(gaugeBitmap),
                                contentDescription = "Gauge",
                                modifier = GlanceModifier.size(76.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", state.attendancePercentage),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(statusColor)
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(1.dp))
                                Box(
                                    modifier = GlanceModifier
                                        .background(ColorProvider(statusBgColor))
                                        .cornerRadius(4.dp)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = statusText,
                                        style = TextStyle(
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorProvider(statusColor)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(12.dp))

                        // Middle: Info items (Presents, Absents, and Target Margin)
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall Attendance",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(onSurfaceColor)
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            
                            Row(modifier = GlanceModifier.fillMaxWidth()) {
                                InfoItemWidget("Presents", "${state.attendedClasses}", isDark, onSurfaceColor)
                                Spacer(modifier = GlanceModifier.width(12.dp))
                                val absents = state.heldClasses - state.attendedClasses
                                val absColor = if (absents > 0) (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)) else onSurfaceColor
                                InfoItemWidget("Absents", "$absents", isDark, absColor)
                            }
                            
                            if (state.targetMargin.isNotEmpty()) {
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = state.targetMargin,
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(onSurfaceVariantColor)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        // Far Right: Dedicated Prediction Card Box (Utilizing empty right section space!)
                        val isCanSkip = !isCritical && state.periodsCanSkip > 0
                        val predBg = if (isCanSkip) {
                            if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                        } else {
                            if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)
                        }
                        val predTextColor = if (isCanSkip) onSurfaceColor else (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626))
                        val predTitle = if (isCanSkip) "Can Skip" else "Need Attend"
                        val predNum = if (isCanSkip) "${state.periodsCanSkip}" else "${state.periodsNeedToAttend}"

                        Box(
                            modifier = GlanceModifier
                                .background(ColorProvider(predBg))
                                .cornerRadius(14.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .clickable(createTargetAction("PREDICTION", studentId)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = predNum,
                                    style = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(predTextColor)
                                    )
                                )
                                Spacer(modifier = GlanceModifier.height(1.dp))
                                Text(
                                    text = predTitle,
                                    style = TextStyle(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(predTextColor)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // 2. TODAY'S ATTENDANCE CARD (Fixed 7-Slot Sequence Grid)
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(cardBg))
                        .cornerRadius(20.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(createTargetAction("TODAYS_REGISTER", studentId))
                ) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "Today's Attendance",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(onSurfaceColor)
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        val rawTimeline = state.todayAttendanceTimeline
                        if (rawTimeline.isEmpty()) {
                            Text(
                                text = "No classes today",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    color = ColorProvider(onSurfaceVariantColor)
                                )
                            )
                        } else {
                            // Standardize to fixed 7-slot daily timetable layout so period sizes never shift
                            val fixedCount = maxOf(7, rawTimeline.size)
                            val fixedTimeline = List(fixedCount) { i ->
                                if (i < rawTimeline.size) rawTimeline[i] else AttendanceStatus.UPCOMING
                            }

                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val chipSizeDp = if (fixedCount >= 8) 18 else 20
                                val periodFontSize = if (fixedCount >= 8) 7.sp else 8.sp
                                val chipFontSize = if (fixedCount >= 8) 9.sp else 10.sp

                                fixedTimeline.forEachIndexed { index, status ->
                                    Box(
                                        modifier = GlanceModifier.defaultWeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        NumberedTimelineChip(
                                            periodNum = index + 1,
                                            status = status,
                                            isDark = isDark,
                                            chipSizeDp = chipSizeDp,
                                            periodFontSize = periodFontSize,
                                            chipFontSize = chipFontSize
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InfoItemWidget(label: String, value: String, isDark: Boolean, valueColor: Color) {
        val onSurfaceVariantColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
        Column {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 8.sp,
                    color = ColorProvider(onSurfaceVariantColor)
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(valueColor)
                )
            )
        }
    }

    private fun createCircularGaugeBitmap(
        percentage: Double,
        threshold: Int,
        isDark: Boolean
    ): Bitmap {
        val sizePx = 200
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val strokeWidth = 16f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            this.strokeWidth = strokeWidth
        }
        
        val rect = RectF(
            strokeWidth / 2f + 4f,
            strokeWidth / 2f + 4f,
            sizePx - strokeWidth / 2f - 4f,
            sizePx - strokeWidth / 2f - 4f
        )
        
        // 1. Draw track
        val trackColor = if (isDark) 0xFF27272A.toInt() else 0xFFE4E4E7.toInt()
        paint.color = trackColor
        canvas.drawArc(rect, 0f, 360f, false, paint)
        
        // 2. Draw active progress
        val indicatorColor = if (percentage >= threshold) {
            if (isDark) 0xFFFAFAFA.toInt() else 0xFF09090B.toInt()
        } else {
            if (isDark) 0xFFEF4444.toInt() else 0xFFDC2626.toInt()
        }
        paint.color = indicatorColor
        val sweepAngle = ((percentage / 100.0) * 360.0).coerceIn(0.0, 360.0).toFloat()
        canvas.drawArc(rect, -90f, sweepAngle, false, paint)
        
        return bitmap
    }

    @Composable
    private fun NumberedTimelineChip(
        periodNum: Int,
        status: AttendanceStatus,
        isDark: Boolean,
        chipSizeDp: Int = 22,
        periodFontSize: androidx.compose.ui.unit.TextUnit = 8.sp,
        chipFontSize: androidx.compose.ui.unit.TextUnit = 10.sp
    ) {
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

        val onSurfaceVariantColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "P$periodNum",
                style = TextStyle(
                    fontSize = periodFontSize,
                    color = ColorProvider(onSurfaceVariantColor)
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Box(
                modifier = GlanceModifier
                    .size(chipSizeDp.dp)
                    .background(ColorProvider(bg))
                    .cornerRadius(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    style = TextStyle(
                        fontSize = chipFontSize,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(textColor),
                        textAlign = TextAlign.Center
                    )
                )
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
        val isRefreshingKey = booleanPreferencesKey("dashboard_is_refreshing")
        val targetScreenKey = ActionParameters.Key<String>("target_screen")
        val studentIdKey = ActionParameters.Key<String>("selected_student_id")

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
        WidgetRefreshHelper.triggerRefresh(context)
    }
}
