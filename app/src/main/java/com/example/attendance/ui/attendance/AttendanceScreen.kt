package com.example.attendance.ui.attendance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.SubjectSummary
import com.example.attendance.data.repository.AttendanceRepository
import java.text.SimpleDateFormat
import java.util.*

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferences
) : ViewModel() {
    val attendance = repository.attendance
    val notificationThreshold: Int
        get() = prefs.notificationThreshold

    var searchQuery = mutableStateOf("")
        private set

    var currentSortOrder = mutableStateOf(SortOrder.ALPHABETICAL)
        private set

    val simulatedLeaves = mutableStateListOf<Long>()
    val simulatedHolidays = mutableStateListOf<Long>()

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        currentSortOrder.value = order
    }

    fun addSimulatedLeave(millis: Long) {
        if (!simulatedLeaves.contains(millis)) {
            simulatedLeaves.add(millis)
            simulatedHolidays.remove(millis)
        }
    }

    fun addSimulatedHoliday(millis: Long) {
        if (!simulatedHolidays.contains(millis)) {
            simulatedHolidays.add(millis)
            simulatedLeaves.remove(millis)
        }
    }

    fun removeSimulatedDate(millis: Long) {
        simulatedLeaves.remove(millis)
        simulatedHolidays.remove(millis)
    }

    fun resetSimulation() {
        simulatedLeaves.clear()
        simulatedHolidays.clear()
    }

    enum class SortOrder {
        ALPHABETICAL,
        PERCENTAGE_ASC,
        PERCENTAGE_DESC
    }
}


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
    val subjects = attendanceState?.subjectwise_summary ?: emptyList()
    val searchQuery by viewModel.searchQuery
    val sortOrder by viewModel.currentSortOrder

    val filteredSortedSubjects = remember(subjects, searchQuery, sortOrder) {
        subjects
            .filter {
                it.subject_name.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith { s1, s2 ->
                when (sortOrder) {
                    AttendanceViewModel.SortOrder.ALPHABETICAL -> s1.subject_name.compareTo(s2.subject_name, ignoreCase = true)
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
                        key = { it.subject_name }
                    ) { subject ->
                        SubjectCard(
                            subject = subject,
                            threshold = threshold,
                            onClick = { onNavigateToSubjectDetails(subject.subject_name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectCard(subject: SubjectSummary, threshold: Int, onClick: () -> Unit) {
    val percentage = subject.percentageDouble
    val isSafe = percentage >= threshold.toDouble()
    val accentColor = if (isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val targetProgress = (percentage / 100f).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "SubjectProgressAnimation"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subject_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = subject.percentage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attended: ${subject.attended} / ${subject.held}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Surface(
                    color = if (isSafe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSafe) "On Track" else "Low Attendance",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSafe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceSimulatorCard(
    simResult: com.example.attendance.data.model.SimulationResult,
    simulatedLeaves: List<Long>,
    onAddLeaveClick: () -> Unit,
    onRemoveDate: (Long) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val isSafe = simResult.simulatedPercentage >= 75.0
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (darkTheme) Color(0xFF1E293B) else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.EventAvailable,
                        contentDescription = "Leave Simulator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Leave & Bunk Simulator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (simulatedLeaves.isNotEmpty()) {
                    TextButton(onClick = onResetClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("Reset", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Block: Skip margin
                Surface(
                    color = if (isSafe) Color(0xFF1B4D2E) else Color(0xFF4A1C1C),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (simResult.simulatedCanSkip > 0) "${simResult.simulatedCanSkip} Periods" else "${simResult.simulatedNeedToAttend} Needed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSafe) Color(0xFF81C784) else Color(0xFFE57373)
                        )
                        Text(
                            text = if (simResult.simulatedCanSkip > 0) "Periods Can Skip" else "Classes Required",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Right Block: Percentage
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f%%", simResult.simulatedPercentage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSafe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (simulatedLeaves.isEmpty()) "Current Attendance" else "Projected Attendance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Simulated Leaves Date Chips
            if (simulatedLeaves.isNotEmpty()) {
                Text(
                    text = "Planned Leaves:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    simulatedLeaves.forEach { millis ->
                        InputChip(
                            selected = true,
                            onClick = { onRemoveDate(millis) },
                            label = { Text(dateFormat.format(Date(millis))) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove date",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onAddLeaveClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select Future Leave Date")
            }
        }
    }
}

