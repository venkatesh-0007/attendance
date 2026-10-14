package com.attendance.app.ui.attendance

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.theme.*
import com.attendance.app.ui.attendance.components.AttendanceSimulatorCard
import com.attendance.app.ui.attendance.components.SubjectCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onNavigateToSubjectDetails: (String) -> Unit,
    onNavigateToTable: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val attendanceState by viewModel.attendance.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val threshold = viewModel.notificationThreshold
    val subjects = attendanceState?.subjectwiseSummary ?: emptyList()
    val searchQuery by viewModel.searchQuery
    val sortOrder by viewModel.currentSortOrder
    val accent = LocalAccentColor.current

    // Smooth rotating animation for the frosted refresh button
    val infiniteTransition = rememberInfiniteTransition(label = "AttendanceRefreshAnim")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AttendanceRotationAngle"
    )
    val animatedRotation = if (isRefreshing) rotation else 0f

    val filteredSortedSubjects = remember(subjects, searchQuery, sortOrder) {
        subjects
            .filter {
                it.subjectName.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith { s1, s2 ->
                when (sortOrder) {
                    AttendanceViewModel.SortOrder.ALPHABETICAL -> s1.subjectName.compareTo(s2.subjectName, ignoreCase = true)
                    AttendanceViewModel.SortOrder.PERCENTAGE_ASC -> s1.percentageDouble.compareTo(s2.percentageDouble)
                    AttendanceViewModel.SortOrder.PERCENTAGE_DESC -> s2.percentageDouble.compareTo(s1.percentageDouble)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Subject Attendance",
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
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

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(accent.copy(alpha = 0.16f))
                                .border(0.75.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        ) {
                            TextButton(
                                onClick = onNavigateToTable,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "View Grid",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                            }
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
            ) {
                val simulatedLeaves = viewModel.simulatedLeaves
                val simulatedHolidays = viewModel.simulatedHolidays
                val simResult = remember(attendanceState, simulatedLeaves.toList(), simulatedHolidays.toList(), threshold) {
                    attendanceState?.calculateSimulation(
                        leaveDates = simulatedLeaves.toSet(),
                        holidayDates = simulatedHolidays.toSet(),
                        targetThreshold = threshold.toDouble()
                    )
                }

                var showDatePicker by remember { mutableStateOf(false) }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        viewModel.addSimulatedLeave(millis)
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("Mark Leave", color = accent, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (simResult != null) {
                    AttendanceSimulatorCard(
                        simResult = simResult,
                        simulatedLeaves = simulatedLeaves,
                        onAddLeaveClick = { showDatePicker = true },
                        onRemoveDate = { viewModel.removeSimulatedDate(it) },
                        onResetClick = { viewModel.resetSimulation() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Search & Sort Bar in Liquid Glass style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search subjects...", fontSize = 13.5.sp, color = Color.White.copy(alpha = 0.45f)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedContainerColor = GlassCardBg,
                            unfocusedContainerColor = GlassCardBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GlassCardBg)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .clickable { sortExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                            modifier = Modifier.background(SpaceNavyDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alphabetical", color = Color.White) },
                                onClick = {
                                    viewModel.updateSortOrder(AttendanceViewModel.SortOrder.ALPHABETICAL)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attendance Ascending", color = Color.White) },
                                onClick = {
                                    viewModel.updateSortOrder(AttendanceViewModel.SortOrder.PERCENTAGE_ASC)
                                    sortExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attendance Descending", color = Color.White) },
                                onClick = {
                                    viewModel.updateSortOrder(AttendanceViewModel.SortOrder.PERCENTAGE_DESC)
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }

                if (filteredSortedSubjects.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No subjects found.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = filteredSortedSubjects,
                            key = { it.subjectName }
                        ) { subject ->
                            SubjectCard(
                                subject = subject,
                                threshold = threshold,
                                onClick = { onNavigateToSubjectDetails(subject.subjectName) }
                            )
                        }
                    }
                }
            }
        }
    }
}
