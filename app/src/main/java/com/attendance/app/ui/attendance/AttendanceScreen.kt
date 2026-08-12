package com.attendance.app.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
    val threshold = viewModel.notificationThreshold
    val subjects = attendanceState?.subjectwiseSummary ?: emptyList()
    val searchQuery by viewModel.searchQuery
    val sortOrder by viewModel.currentSortOrder

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
                title = { Text("Subject Attendance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToTable) {
                        Text("View Table", style = MaterialTheme.typography.labelLarge)
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
                            Text("Mark Leave")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search subjects...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { sortExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Options")
                    }

                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Alphabetical") },
                            onClick = {
                                viewModel.updateSortOrder(AttendanceViewModel.SortOrder.ALPHABETICAL)
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Attendance Ascending") },
                            onClick = {
                                viewModel.updateSortOrder(AttendanceViewModel.SortOrder.PERCENTAGE_ASC)
                                sortExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Attendance Descending") },
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
                    Text("No subjects found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
