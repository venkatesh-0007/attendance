package com.attendance.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.theme.*
import com.attendance.app.ui.attendance.AttendanceViewModel
import com.attendance.app.ui.dashboard.components.LiquidGlassCard
import com.attendance.app.util.PdfExporter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val attendanceState by viewModel.attendance.collectAsState()
    val accent = LocalAccentColor.current

    val calendar = remember { Calendar.getInstance() }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time) }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthCalendar = remember(calendar, attendanceState) {
        val list = mutableListOf<CalendarDayInfo>()
        val calCopy = calendar.clone() as Calendar
        val currentMonth = calCopy.get(Calendar.MONTH)
        val currentYear = calCopy.get(Calendar.YEAR)

        for (day in 1..daysInMonth) {
            calCopy.set(currentYear, currentMonth, day)
            val dateStr = String.format(Locale.US, "%02d/%02d", day, currentMonth + 1)
            val (presents, absents) = attendanceState?.getTodaySummary(dateStr) ?: (0 to 0)

            val status = when {
                presents > 0 && absents == 0 -> DayStatus.ALL_PRESENT
                absents > 0 -> DayStatus.HAS_ABSENT
                else -> DayStatus.NO_CLASSES
            }
            list.add(CalendarDayInfo(day, dateStr, status, presents, absents))
        }
        list
    }

    var selectedDayInfo by remember { mutableStateOf<CalendarDayInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Attendance Calendar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    attendanceState?.let { data ->
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f))
                                .border(0.5.dp, accent.copy(alpha = 0.35f), CircleShape)
                                .clickable { PdfExporter.generateAndSharePdf(context, data) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Export PDF",
                                tint = accent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceNavyDeep.copy(alpha = 0.85f)
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
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
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Month Header in Liquid Glass
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMonthName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LegendBadge("Present", MacSafe)
                            LegendBadge("Absent", MacCritical)
                        }
                    }
                }

                // Days of week header
                val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    daysOfWeek.forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Calendar Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(monthCalendar, key = { it.dayNumber }) { dayInfo ->
                        CalendarDayCell(dayInfo = dayInfo, onClick = { selectedDayInfo = dayInfo })
                    }
                }
            }
        }
    }

    // Selected Day Breakdown Dialog
    selectedDayInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { selectedDayInfo = null },
            containerColor = SpaceNavyDark,
            shape = RoundedCornerShape(20.dp),
            confirmButton = {
                TextButton(onClick = { selectedDayInfo = null }) {
                    Text("Close", color = accent, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "Date: ${info.dateStr}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Classes Attended:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                        Text("${info.presents} Periods", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MacSafe)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Classes Missed:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            "${info.absents} Periods",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (info.absents > 0) MacCritical else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        )
    }
}

enum class DayStatus { ALL_PRESENT, HAS_ABSENT, NO_CLASSES }

data class CalendarDayInfo(
    val dayNumber: Int,
    val dateStr: String,
    val status: DayStatus,
    val presents: Int,
    val absents: Int
)

@Composable
fun CalendarDayCell(dayInfo: CalendarDayInfo, onClick: () -> Unit) {
    val dotColor = when (dayInfo.status) {
        DayStatus.ALL_PRESENT -> MacSafe
        DayStatus.HAS_ABSENT -> MacCritical
        DayStatus.NO_CLASSES -> Color.White.copy(alpha = 0.15f)
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(GlassCardBg)
            .border(
                0.75.dp,
                Brush.linearGradient(listOf(GlassCardBorderTop, GlassCardBorderMid, GlassCardBorderBottom)),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${dayInfo.dayNumber}",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .shadow(elevation = if (dayInfo.status != DayStatus.NO_CLASSES) 2.dp else 0.dp, shape = CircleShape, spotColor = dotColor)
            )
        }
    }
}

@Composable
fun LegendBadge(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}
