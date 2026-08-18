package com.attendance.app.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.data.model.AttendanceStatus
import com.attendance.app.ui.dashboard.components.AttendanceGauge
import com.attendance.app.ui.dashboard.components.InfoItem
import com.attendance.app.ui.dashboard.components.MenuCard
import com.attendance.app.ui.dashboard.components.PeriodCircle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val currentAccountName by viewModel.currentAccountName.collectAsState()

    val scrollState = rememberScrollState()

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
                        currentAccountName?.let {
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
            lastUpdated.takeIf { it > 0 }?.let { timestamp ->
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
                                        value = "${currentData.totalInfo?.totalAttended ?: 0}"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .width(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
                                    )
                                    InfoItem(
                                        label = "Total Held",
                                        value = "${currentData.totalInfo?.totalHeld ?: 0}"
                                    )
                                }
                            }
                        }

                        // Dynamic prediction card
                        val percentage = currentData.overallPercentage
                        val attended = currentData.totalInfo?.totalAttended ?: 0
                        val held = currentData.totalInfo?.totalHeld ?: 0
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
                                                AttendanceStatus.PRESENT -> 'P'
                                                AttendanceStatus.ABSENT -> 'A'
                                                AttendanceStatus.HOLIDAY -> 'H'
                                                AttendanceStatus.LEAVE -> 'L'
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
