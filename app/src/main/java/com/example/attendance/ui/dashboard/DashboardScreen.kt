package com.example.attendance.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferences
) : ViewModel() {

    val attendance = repository.attendance

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val lastUpdated: Long
        get() = prefs.lastUpdated

    val notificationThreshold: Int
        get() = prefs.notificationThreshold

    fun refresh() {
        val studentId = prefs.studentId ?: return
        val password = prefs.password ?: return

        viewModelScope.launch {
            _isRefreshing.value = true
            repository.fetchAttendance(studentId, password)
            _isRefreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAttendance: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTable: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val attendanceState by viewModel.attendance.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val scrollState = rememberScrollState()

    LaunchedEffect(attendanceState) {
        attendanceState?.attendance_table?.let { table ->
            println("ATT_DEBUG HEADERS: ${table.headers}")
            println("ATT_DEBUG ROWS: ${table.rows}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        attendanceState?.student_name?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar & Export")
                    }
                    IconButton(onClick = { viewModel.refresh() }, enabled = !isRefreshing) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            viewModel.lastUpdated.takeIf { it > 0 }?.let { timestamp ->
                val format = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                Text(
                    text = "Last updated: ${format.format(Date(timestamp))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val currentData = attendanceState
            if (currentData == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Fetching attendance data...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(500)) + slideInVertically(animationSpec = tween(500), initialOffsetY = { 60 })
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val overall = currentData.overallPercentage
                        val threshold = viewModel.notificationThreshold
                        val isBelow = overall < threshold
                        val bannerText = if (isBelow) {
                            "Warning: Your attendance (${String.format(Locale.getDefault(), "%.2f", overall)}%) is below your target threshold ($threshold%)."
                        } else {
                            "Good Standing: Your attendance (${String.format(Locale.getDefault(), "%.2f", overall)}%) is above your target threshold ($threshold%)."
                        }
                        val bannerColor = if (isBelow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        val bannerTextColor = if (isBelow) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = bannerColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = bannerTextColor
                                )
                            }
                        }

                        // Attendance Gauge Card
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                AttendanceGauge(
                                    percentage = currentData.overallPercentage,
                                    threshold = viewModel.notificationThreshold
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    InfoItem(
                                        label = "Attended Classes",
                                        value = "${currentData.total_info?.total_attended ?: 0}"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .width(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    InfoItem(
                                        label = "Total Held",
                                        value = "${currentData.total_info?.total_held ?: 0}"
                                    )
                                }
                            }
                        }

                        // Calculate predictions dynamically based on user configured target threshold
                        val percentage = currentData.overallPercentage
                        val attended = currentData.total_info?.total_attended ?: 0
                        val held = currentData.total_info?.total_held ?: 0
                        val targetThreshold = viewModel.notificationThreshold.toDouble()
                        val targetFactor = targetThreshold / 100.0

                        val periodsCanSkip = if (held > 0 && percentage >= targetThreshold) {
                            val calculatedCanSkip = kotlin.math.floor((attended - targetFactor * held) / targetFactor).toInt()
                            maxOf(0, calculatedCanSkip)
                        } else 0

                        val periodsNeedToAttend = if (held > 0 && percentage < targetThreshold) {
                            val divisor = 1.0 - targetFactor
                            val calculatedNeedToAttend = if (divisor > 0.0) {
                                kotlin.math.ceil((targetFactor * held - attended) / divisor).toInt()
                            } else 0
                            maxOf(0, calculatedNeedToAttend)
                        } else 0

                        val isBelowThreshold = percentage < targetThreshold
                        val predBgColor = if (isBelowThreshold) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                        val predTextColor = if (isBelowThreshold) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        val predTitle = if (isBelowThreshold) "Required Attendance" else "Safe to Skip"
                        val predValue = if (isBelowThreshold) "$periodsNeedToAttend Periods" else "$periodsCanSkip Periods"
                        val predSubtitle = if (isBelowThreshold) {
                            "You need to attend this many classes to reach your ${viewModel.notificationThreshold}% target"
                        } else {
                            "You can miss this many classes and remain above your ${viewModel.notificationThreshold}% target"
                        }

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = predBgColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = predTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = predTextColor.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = if (isBelowThreshold) "🔴 Target Alert" else "🟢 Safe Status",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = predTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = predValue,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = predTextColor
                                )
                                Text(
                                    text = predSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = predTextColor.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Today's Status Badges Card
                        val todaySummary = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date())
                        val todayTimeline = currentData.getTodayAttendanceTimeline(todaySummary)
                        val countSummary = currentData.getTodayCountSummary(todaySummary)

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToTable() },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Today's Attendance Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "View Grid →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (todayTimeline.isEmpty()) {
                                    Text(
                                        text = "No classes recorded for today • Tap to view grid",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = countSummary.toDisplayString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        todayTimeline.forEachIndexed { index, status ->
                                            val charStatus = when (status) {
                                                com.example.attendance.data.model.AttendanceStatus.PRESENT -> 'P'
                                                com.example.attendance.data.model.AttendanceStatus.ABSENT -> 'A'
                                                com.example.attendance.data.model.AttendanceStatus.HOLIDAY -> 'H'
                                                com.example.attendance.data.model.AttendanceStatus.LEAVE -> 'L'
                                                else -> '-'
                                            }
                                            PeriodCircle(periodNumber = index + 1, status = charStatus)
                                        }
                                    }
                                }
                            }
                        }

                        // Menu Navigation Grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MenuCard(
                                    title = "Subject Breakdown",
                                    icon = Icons.AutoMirrored.Filled.List,
                                    description = "Subject details",
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToAttendance
                                )
                                MenuCard(
                                    title = "Attendance Grid",
                                    icon = Icons.Default.GridView,
                                    description = "Full register table",
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToTable
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MenuCard(
                                    title = "Class Schedule",
                                    icon = Icons.Default.DateRange,
                                    description = "Weekly timetable",
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToTimetable
                                )
                                MenuCard(
                                    title = "Attendance Calendar",
                                    icon = Icons.Default.CalendarMonth,
                                    description = "Monthly history",
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToCalendar
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MenuCard(
                                    title = "Settings",
                                    icon = Icons.Default.Settings,
                                    description = "Preferences & threshold",
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToSettings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceGauge(percentage: Double, threshold: Int, modifier: Modifier = Modifier) {
    val targetProgress = (percentage / 100f).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "GaugeAnimation"
    )
    val indicatorColor = if (percentage >= threshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(160.dp),
            strokeWidth = 12.dp,
            color = indicatorColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.getDefault(), "%.2f%%", percentage),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Overall Attendance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PeriodCircle(periodNumber: Int, status: Char) {
    val containerColor = when (status.uppercaseChar()) {
        'P' -> MaterialTheme.colorScheme.primaryContainer
        'A' -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (status.uppercaseChar()) {
        'P' -> MaterialTheme.colorScheme.onPrimaryContainer
        'A' -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(containerColor, CircleShape)
    ) {
        Text(
            text = status.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun MenuCard(
    title: String,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
