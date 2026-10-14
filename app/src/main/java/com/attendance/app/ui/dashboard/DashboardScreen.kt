package com.attendance.app.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.theme.*
import com.attendance.app.ui.dashboard.components.GlassMenuCard
import com.attendance.app.ui.dashboard.components.PresentAttendanceGlassCard
import com.attendance.app.ui.dashboard.components.SafeToSkipGlassCard
import com.attendance.app.ui.dashboard.components.TodayAttendanceGlassCard
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
    val accent = LocalAccentColor.current

    val scrollState = rememberScrollState()

    // Smooth rotating animation for the glass refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "RefreshRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RotationAngle"
    )
    val animatedRotation = if (isRefreshing) rotation else 0f

    // Mac-style Clean Roll Number (strip leading colons, spaces, uppercase)
    val rawRoll = attendanceState?.rollNumber ?: ""
    val cleanRoll = rawRoll.trim().trimStart(':').trim().uppercase()
    val studentDisplayName = currentAccountName ?: attendanceState?.studentName ?: "Student"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SpaceNavyDeep,
                        SpaceNavyDark,
                        SpaceNavyMid
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // MARK: - Mac-style Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Student Name & Cleaned Roll Number Capsule
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = studentDisplayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (cleanRoll.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(7.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = cleanRoll,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // Right: Action Buttons (Calendar & Frosted Rotating Refresh)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Frosted Calendar Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { onNavigateToCalendar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // Frosted Rotating Refresh Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable(enabled = !isRefreshing) { viewModel.refresh() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isRefreshing) accent else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(17.dp)
                                .graphicsLayer { rotationZ = animatedRotation }
                        )
                    }
                }
            }

            // Optional Last Updated Timestamp
            lastUpdated.takeIf { it > 0 }?.let { timestamp ->
                val format = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                Text(
                    text = "Last updated: ${format.format(Date(timestamp))}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            val currentData = attendanceState
            if (currentData == null) {
                // Glass Loading State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "Fetching attendance data...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                val targetThreshold = viewModel.notificationThreshold.toDouble()

                // MARK: - 1. PRESENT ATTENDANCE (Liquid Glass Tile)
                PresentAttendanceGlassCard(
                    attendance = currentData,
                    targetThreshold = targetThreshold,
                    modifier = Modifier.fillMaxWidth()
                )

                // MARK: - 2. SAFE TO SKIP PERIODS (Liquid Glass Tile)
                SafeToSkipGlassCard(
                    attendance = currentData,
                    targetThreshold = targetThreshold,
                    modifier = Modifier.fillMaxWidth()
                )

                // MARK: - 3. TODAY'S ATTENDANCE FROM ATTENDANCE GRID (Liquid Glass Tile)
                TodayAttendanceGlassCard(
                    attendance = currentData,
                    modifier = Modifier.fillMaxWidth(),
                    onNavigateToGrid = onNavigateToTable
                )

                Spacer(modifier = Modifier.height(2.dp))

                // MARK: - Quick Navigation Label
                Text(
                    text = "ACTIONS & DETAILS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                )

                // MARK: - Liquid Glass Navigation Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassMenuCard(
                            title = "Subject Breakdown",
                            description = "Subject stats & details",
                            icon = Icons.AutoMirrored.Filled.List,
                            iconTint = accent,
                            iconBgColor = accent.copy(alpha = 0.16f),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAttendance
                        )
                        GlassMenuCard(
                            title = "Attendance Grid",
                            description = "Full register table",
                            icon = Icons.Default.GridView,
                            iconTint = MacPurple,
                            iconBgColor = MacPurple.copy(alpha = 0.16f),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToTable
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassMenuCard(
                            title = "Class Schedule",
                            description = "Weekly timetable",
                            icon = Icons.Default.DateRange,
                            iconTint = MacCyan,
                            iconBgColor = MacCyan.copy(alpha = 0.16f),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToTimetable
                        )
                        GlassMenuCard(
                            title = "Calendar",
                            description = "Monthly history",
                            icon = Icons.Default.CalendarMonth,
                            iconTint = MacSafe,
                            iconBgColor = MacSafe.copy(alpha = 0.16f),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCalendar
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        GlassMenuCard(
                            title = "Settings",
                            description = "Preferences & threshold",
                            icon = Icons.Default.Settings,
                            iconTint = Color.White.copy(alpha = 0.9f),
                            iconBgColor = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSettings
                        )
                    }
                }
            }
        }
    }
}
