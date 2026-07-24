package com.example.attendance.ui.attendance

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.ViewModel
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.SubjectSummary
import com.example.attendance.data.repository.AttendanceRepository
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

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        currentSortOrder.value = order
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
                    items(filteredSortedSubjects) { subject ->
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
