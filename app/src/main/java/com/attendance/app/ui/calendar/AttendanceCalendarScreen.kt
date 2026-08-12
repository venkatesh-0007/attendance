package com.attendance.app.ui.calendar

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.data.model.AttendanceResponse
import com.attendance.app.ui.attendance.AttendanceViewModel
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

    val calendar = remember { Calendar.getInstance() }
    val currentMonthName = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time) }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val monthCalendar = remember(calendar) {
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
                title = { Text("Attendance Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    attendanceState?.let { data ->
                        IconButton(onClick = { PdfExporter.generateAndSharePdf(context, data) }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Header
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentMonthName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendBadge("Present", MaterialTheme.colorScheme.primary)
                        LegendBadge("Absent", MaterialTheme.colorScheme.error)
                    }
                }
            }

            // Days of week header
            val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                items(monthCalendar) { dayInfo ->
                    CalendarDayCell(dayInfo = dayInfo, onClick = { selectedDayInfo = dayInfo })
                }
            }
        }
    }

    // Selected Day Breakdown Dialog
    selectedDayInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { selectedDayInfo = null },
            confirmButton = {
                TextButton(onClick = { selectedDayInfo = null }) {
                    Text("Close")
                }
            },
            title = { Text("Date: ${info.dateStr}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Classes Attended: ${info.presents}", style = MaterialTheme.typography.bodyMedium)
                    Text("Classes Missed: ${info.absents}", style = MaterialTheme.typography.bodyMedium, color = if (info.absents > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
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
        DayStatus.ALL_PRESENT -> MaterialTheme.colorScheme.primary
        DayStatus.HAS_ABSENT -> MaterialTheme.colorScheme.error
        DayStatus.NO_CLASSES -> MaterialTheme.colorScheme.surfaceVariant
    }

    ElevatedCard(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${dayInfo.dayNumber}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
fun LegendBadge(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
