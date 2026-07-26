package com.example.attendance.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceTableScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val attendanceState by viewModel.attendance.collectAsState()
    val table = attendanceState?.attendance_table

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Grid", fontWeight = FontWeight.Bold) },
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
        if (table == null || table.rows.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No attendance table data available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val headers = table.headers ?: emptyList()
            val rows = table.rows ?: emptyList()

            val subjectHeader = headers.getOrNull(0) ?: "Subject"
            val dataHeaders = if (headers.size > 1) headers.subList(1, headers.size) else emptyList()

            val lazyListState = rememberLazyListState()
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // -------------------------------------------------------------
                // LEFT STICKY COLUMN: Subject Names
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Sticky Header Cell
                    GridCell(
                        text = subjectHeader,
                        isHeader = true,
                        alignStart = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Subject Column LazyList
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = lazyListState
                    ) {
                        items(rows) { row ->
                            val subjectName = row.getOrNull(0) ?: ""
                            GridCell(
                                text = subjectName,
                                isHeader = false,
                                isSubjectName = true,
                                alignStart = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }

                // Vertical Divider separating Sticky Subject column from scrollable grid
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // -------------------------------------------------------------
                // RIGHT SCROLLABLE COLUMNS: Dates & Totals
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        dataHeaders.forEach { headerText ->
                            val cellWidth = getColumnWidth(headerText)
                            GridCell(
                                text = headerText,
                                isHeader = true,
                                modifier = Modifier
                                    .width(cellWidth)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Synchronized Scrollable Data Grid LazyList
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = lazyListState
                    ) {
                        items(rows) { row ->
                            val dataCells = if (row.size > 1) row.subList(1, row.size) else emptyList()
                            Row(modifier = Modifier.height(48.dp)) {
                                dataCells.forEachIndexed { colIdx, cellText ->
                                    val headerText = dataHeaders.getOrNull(colIdx) ?: ""
                                    val cellWidth = getColumnWidth(headerText)
                                    GridCell(
                                        text = cellText,
                                        isHeader = false,
                                        modifier = Modifier
                                            .width(cellWidth)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

private fun getColumnWidth(headerText: String): Dp {
    val clean = headerText.trim()
    return when {
        clean.contains("Atted", ignoreCase = true) || clean.contains("Held", ignoreCase = true) -> 90.dp
        clean == "%" -> 70.dp
        else -> 64.dp
    }
}

@Composable
private fun GridCell(
    text: String,
    isHeader: Boolean = false,
    isSubjectName: Boolean = false,
    alignStart: Boolean = false,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val trimmed = text.trim()

    // Determine status colors for Dark and Light themes
    val (bgColor, textColor) = when {
        isHeader -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        isSubjectName -> Pair(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface
        )
        trimmed.startsWith("P") -> if (darkTheme) {
            Pair(Color(0xFF1B4D2E), Color(0xFF81C784))
        } else {
            Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        }
        trimmed.startsWith("A") -> if (darkTheme) {
            Pair(Color(0xFF4A1C1C), Color(0xFFE57373))
        } else {
            Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        }
        trimmed.startsWith("H") -> if (darkTheme) {
            Pair(Color(0xFF1A365D), Color(0xFF64B5F6))
        } else {
            Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        }
        trimmed.startsWith("L") -> if (darkTheme) {
            Pair(Color(0xFF3B1E54), Color(0xFFBA68C8))
        } else {
            Pair(Color(0xFFF3E5F5), Color(0xFF6A1B9A))
        }
        trimmed.toDoubleOrNull() != null -> {
            val pct = trimmed.toDouble()
            if (pct >= 75.0) {
                Pair(Color.Transparent, if (darkTheme) Color(0xFF81C784) else Color(0xFF2E7D32))
            } else {
                Pair(Color.Transparent, if (darkTheme) Color(0xFFE57373) else Color(0xFFC62828))
            }
        }
        else -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurface)
    }

    Surface(
        modifier = modifier,
        color = bgColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.Center
        ) {
            Text(
                text = trimmed,
                fontWeight = if (isHeader || trimmed.toDoubleOrNull() != null) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isSubjectName) 11.sp else 12.sp,
                color = textColor,
                textAlign = if (alignStart) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
