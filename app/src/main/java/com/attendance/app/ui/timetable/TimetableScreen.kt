package com.attendance.app.ui.timetable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.data.model.FacultyInfo
import com.attendance.app.data.model.TimetableClass
import com.attendance.app.ui.attendance.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val attendanceState by viewModel.attendance.collectAsState()

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val todayName = remember { SimpleDateFormat("EEEE", Locale.getDefault()).format(Date()) }

    var selectedDay by remember {
        mutableStateOf(if (days.contains(todayName)) todayName else "Monday")
    }

    val timetableData = attendanceState?.timetable ?: emptyList()
    val facultyData = attendanceState?.facultyInformation ?: emptyList()

    val selectedDayClasses = (timetableData.firstOrNull {
        it.day.equals(selectedDay, ignoreCase = true)
    }?.classes ?: emptyList()).sortedBy { getStartMinutes(it.time) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Schedule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(days) { day ->
                        val isSelected = day == selectedDay
                        val isToday = day == todayName

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = day },
                            label = {
                                Text(
                                    text = day.take(3),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedDay's Classes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedDay == todayName) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Today", style = MaterialTheme.typography.labelMedium) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null
                        )
                    }
                }
            }

            if (selectedDayClasses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No classes scheduled for $selectedDay.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(selectedDayClasses) { scheduleClass ->
                    ClassScheduleCard(scheduleClass = scheduleClass, isToday = selectedDay == todayName)
                }
            }

            if (facultyData.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Faculty Directory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(facultyData) { faculty ->
                    FacultyCard(faculty = faculty)
                }
            }
        }
    }
}

@Composable
fun ClassScheduleCard(scheduleClass: TimetableClass, isToday: Boolean) {
    val isOngoing = isToday && isClassOngoing(scheduleClass.time)

    val cardBg = when {
        isOngoing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isOngoing) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scheduleClass.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isOngoing) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ONGOING",
                                color = MaterialTheme.colorScheme.onError,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = scheduleClass.time,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                scheduleClass.faculty?.let { faculty ->
                    Text(
                        text = "Faculty: $faculty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                scheduleClass.room?.let { room ->
                    Text(
                        text = "Room: $room",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FacultyCard(faculty: FacultyInfo) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = faculty.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    faculty.subject?.let { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    faculty.email?.takeIf { it.isNotBlank() }?.let { email ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    faculty.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

private fun getStartMinutes(timeString: String): Int {
    try {
        val parts = timeString.split("-")
        if (parts.isEmpty()) return 0
        val startStr = parts[0].trim()
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        val date = sdf.parse(startStr) ?: return 0
        val cal = java.util.Calendar.getInstance().apply { time = date }
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    } catch (e: Exception) {
        return 0
    }
}

private fun isClassOngoing(timeString: String): Boolean {
    try {
        val parts = timeString.split("-")
        if (parts.size != 2) return false
        val startStr = parts[0].trim()
        val endStr = parts[1].trim()

        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        val startTime = sdf.parse(startStr) ?: return false
        val endTime = sdf.parse(endStr) ?: return false

        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        val currentTimeStr = String.format(Locale.US, "%02d:%02d", currentHour, currentMinute)
        val current24Sdf = SimpleDateFormat("HH:mm", Locale.US)
        val currentTime = current24Sdf.parse(currentTimeStr) ?: return false

        val startCal = java.util.Calendar.getInstance().apply { time = startTime }
        val endCal = java.util.Calendar.getInstance().apply { time = endTime }
        val compCal = java.util.Calendar.getInstance().apply { time = currentTime }

        val startMinutes = startCal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + startCal.get(java.util.Calendar.MINUTE)
        val endMinutes = endCal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + endCal.get(java.util.Calendar.MINUTE)
        val currentMinutes = compCal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + compCal.get(java.util.Calendar.MINUTE)

        return currentMinutes in startMinutes..endMinutes
    } catch (e: Exception) {
        return false
    }
}
