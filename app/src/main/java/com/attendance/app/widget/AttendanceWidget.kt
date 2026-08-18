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

class AttendanceWidget : GlanceAppWidget() {

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
                SmallWidgetContent(widgetState, isRefreshing, isDark, securePrefs, selectedStudentId)
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(
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
            .cornerRadius(20.dp)
            .padding(12.dp)

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
                        modifier = GlanceModifier.clickable(createTargetAction("DASHBOARD", studentId))
                    )
                }
            } else {
                val isCritical = state.attendanceStatus == OverallAttendanceStatus.CRITICAL
                val statusColor = if (isCritical) (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626)) else onSurfaceColor
                val statusBgColor = if (isCritical) (if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)) else (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5))
                val statusBadgeTextColor = if (isCritical) (if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)) else (if (isDark) Color(0xFF34D399) else Color(0xFF059669))

                // 1. TOP SECTION: Student Name, Timestamp, Percentage & Refresh Row
                val headerTitle = if (state.studentName.isNotBlank()) state.studentName else "Attendance"

                // Top Line: Student Name (Left) | Last Updated + Refresh (Right)
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerTitle,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(onSurfaceColor)
                        ),
                        maxLines = 1,
                        modifier = GlanceModifier.defaultWeight().clickable(createTargetAction("DASHBOARD", studentId))
                    )

                    if (state.lastUpdated.isNotEmpty()) {
                        Text(
                            text = state.lastUpdated,
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = ColorProvider(onSurfaceVariantColor)
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(6.dp))
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

                Spacer(modifier = GlanceModifier.height(2.dp))

                // Second Line: Percentage + Status Badge
                Row(
                    modifier = GlanceModifier.fillMaxWidth().clickable(createTargetAction("DASHBOARD", studentId)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", state.attendancePercentage),
                        style = TextStyle(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(statusColor)
                        )
                    )

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(statusBgColor))
                            .cornerRadius(6.dp)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCritical) "CRITICAL" else "SAFE",
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(statusBadgeTextColor)
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 2. MIDDLE SECTION: Highlighted Prediction Card
                val isCanSkip = !isCritical && state.periodsCanSkip > 0
                val skipBg = if (isCanSkip) {
                    cardBg
                } else {
                    if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)
                }
                val skipTextColor = if (isCanSkip) onSurfaceColor else (if (isDark) Color(0xFFEF4444) else Color(0xFFDC2626))
                val skipSubTextColor = if (isCanSkip) onSurfaceVariantColor else (if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C))

                val mainNumberStr = if (isCanSkip) "${state.periodsCanSkip}" else "${state.periodsNeedToAttend}"
                val titleStr = if (isCanSkip) "Periods Can Skip" else "Need Attend"
                val subtitleStr = if (isCanSkip) "Safe to miss without drop" else "Required for target margin"

                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(skipBg))
                        .cornerRadius(14.dp)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .clickable(createTargetAction("PREDICTION", studentId)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        val badgeBg = if (isCanSkip) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else (if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2))
                        val badgeTextClr = if (isCanSkip) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFF87171) else Color(0xFFDC2626))

                        Box(
                            modifier = GlanceModifier
                                .background(ColorProvider(badgeBg))
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mainNumberStr,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(badgeTextClr)
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(10.dp))

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = titleStr,
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(skipTextColor)
                                )
                            )
                            Spacer(modifier = GlanceModifier.height(1.dp))
                            Text(
                                text = subtitleStr,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    color = ColorProvider(skipSubTextColor)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                // 3. BOTTOM SECTION: Today's Attendance Card (Fixed 7-Slot Grid)
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ColorProvider(cardBg))
                        .cornerRadius(14.dp)
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                        .clickable(createTargetAction("TODAYS_REGISTER", studentId))
                ) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "Today's Attendance",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(onSurfaceVariantColor)
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(5.dp))

                        val rawTimeline = state.todayAttendanceTimeline
                        if (rawTimeline.isEmpty()) {
                            Text(
                                text = "No classes today",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = ColorProvider(onSurfaceVariantColor)
                                )
                            )
                        } else {
                            val fixedCount = maxOf(7, rawTimeline.size)
                            val fixedTimeline = List(fixedCount) { i ->
                                if (i < rawTimeline.size) rawTimeline[i] else AttendanceStatus.UPCOMING
                            }

                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                fixedTimeline.forEach { status ->
                                    Box(
                                        modifier = GlanceModifier.defaultWeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CompactChip(
                                            status = status,
                                            isDark = isDark,
                                            chipSizeDp = 18,
                                            fontSize = 9.sp
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
    private fun CompactChip(
        status: AttendanceStatus,
        isDark: Boolean,
        chipSizeDp: Int = 16,
        fontSize: androidx.compose.ui.unit.TextUnit = 9.sp
    ) {
        val (bg, textColor, symbol) = when (status) {
            AttendanceStatus.PRESENT -> Triple(
                if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                "P"
            )
            AttendanceStatus.ABSENT -> Triple(
                if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2),
                if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                "A"
            )
            AttendanceStatus.HOLIDAY -> Triple(
                if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
                if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                "H"
            )
            AttendanceStatus.LEAVE -> Triple(
                if (isDark) Color(0xFF451A03) else Color(0xFFFFEDD5),
                if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
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
                .size(chipSizeDp.dp)
                .background(ColorProvider(bg))
                .cornerRadius(9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = ColorProvider(textColor),
                    textAlign = TextAlign.Center
                )
            )
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
        val isRefreshingKey = booleanPreferencesKey("is_refreshing")
        val targetScreenKey = ActionParameters.Key<String>("target_screen")
        val studentIdKey = ActionParameters.Key<String>("selected_student_id")

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
