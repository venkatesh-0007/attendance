package com.attendance.app.ui.timetable

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.data.model.FacultyInfo
import com.attendance.app.data.model.TimetableClass
import com.attendance.app.theme.*
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
    val accent = LocalAccentColor.current

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
                title = {
                    Text(
                        "Class Schedule",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Day selector pills
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(days) { day ->
                            val isSelected = day == selectedDay
                            val isToday = day == todayName

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) accent else Color.White.copy(alpha = 0.06f))
                                    .border(
                                        if (isSelected) 1.dp else 0.5.dp,
                                        if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedDay = day }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = day.take(3),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f)
                                    )
                                    if (isToday) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color.White else accent)
                                        )
                                    }
                                }
                            }
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
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (selectedDay == todayName) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.copy(alpha = 0.16f))
                                    .border(0.75.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "TODAY",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
                        }
                    }
                }

                if (selectedDayClasses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(GlassCardBg)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No classes scheduled for $selectedDay.",
                                fontSize = 13.5.sp,
                                color = Color.White.copy(alpha = 0.5f)
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "FACULTY DIRECTORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }

                    items(facultyData) { faculty ->
                        FacultyCard(faculty = faculty)
                    }
                }
            }
        }
    }
}

@Composable
fun ClassScheduleCard(scheduleClass: TimetableClass, isToday: Boolean) {
    val isOngoing = isToday && isClassOngoing(scheduleClass.time)
    val accent = LocalAccentColor.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(if (isOngoing) accent.copy(alpha = 0.12f) else GlassCardBg)
            .border(
                1.dp,
                if (isOngoing) Brush.linearGradient(listOf(accent.copy(alpha = 0.5f), accent.copy(alpha = 0.2f)))
                else Brush.linearGradient(listOf(GlassCardBorderTop, GlassCardBorderMid, GlassCardBorderBottom)),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scheduleClass.subject,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isOngoing) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MacCritical)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .border(0.5.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = scheduleClass.time,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = accent
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                scheduleClass.faculty?.let { faculty ->
                    Text(
                        text = "Prof: $faculty",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
                scheduleClass.room?.let { room ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Room $room",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FacultyCard(faculty: FacultyInfo) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val accent = LocalAccentColor.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassCardBg)
            .border(
                1.dp,
                Brush.linearGradient(listOf(GlassCardBorderTop, GlassCardBorderMid, GlassCardBorderBottom)),
                RoundedCornerShape(16.dp)
            )
            .clickable { expanded = !expanded }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = faculty.name,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = faculty.subject ?: "Faculty Member",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!faculty.phone.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.16f))
                                .border(0.5.dp, accent.copy(alpha = 0.35f), CircleShape)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${faculty.phone}"))
                                    context.startActivity(intent)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = accent, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (!faculty.email.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MacPurple.copy(alpha = 0.16f))
                                .border(0.5.dp, MacPurple.copy(alpha = 0.35f), CircleShape)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${faculty.email}"))
                                    context.startActivity(intent)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = MacPurple, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!faculty.phone.isNullOrBlank()) {
                        Text("Phone: ${faculty.phone}", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    if (!faculty.email.isNullOrBlank()) {
                        Text("Email: ${faculty.email}", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

private fun getStartMinutes(timeRange: String): Int {
    return try {
        val startTime = timeRange.split("-").firstOrNull()?.trim() ?: return 0
        val parts = startTime.split(":")
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val isPm = startTime.contains("PM", ignoreCase = true) && hours < 12
        (if (isPm) hours + 12 else hours) * 60 + minutes
    } catch (e: Exception) {
        0
    }
}

private fun isClassOngoing(timeRange: String): Boolean {
    return try {
        val parts = timeRange.split("-")
        if (parts.size < 2) return false
        val now = java.util.Calendar.getInstance()
        val currentMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
        val startMinutes = getStartMinutes(parts[0])
        val endMinutes = getStartMinutes(parts[1])
        currentMinutes in startMinutes..endMinutes
    } catch (e: Exception) {
        false
    }
}
