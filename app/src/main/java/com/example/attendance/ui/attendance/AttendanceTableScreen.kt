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
        val cleanTable = attendanceState?.getCleanTable()
        if (cleanTable == null || cleanTable.rows.isEmpty()) {
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
            val headers = cleanTable.headers
            val rows = cleanTable.rows

            val subjectHeader = headers.getOrNull(1) ?: "Subject"
            val allDataHeaders = if (headers.size > 2) headers.subList(2, headers.size) else emptyList()

            // Separate Date columns from Summary columns (Atted/Held and %)
            val summaryHeaderIndices = allDataHeaders.mapIndexedNotNull { idx, h ->
                val clean = h.trim().lowercase()
                if (clean.contains("%") || clean.contains("atted") || clean.contains("held")) idx else null
            }

            val dateHeaders = if (summaryHeaderIndices.isNotEmpty()) {
                allDataHeaders.filterIndexed { idx, _ -> idx !in summaryHeaderIndices }
            } else allDataHeaders

            val summaryHeaders = if (summaryHeaderIndices.isNotEmpty()) {
                allDataHeaders.filterIndexed { idx, _ -> idx in summaryHeaderIndices }
            } else emptyList()

            val lazyListState = rememberLazyListState()
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color(0xFF0B0F19))
            ) {
                // -------------------------------------------------------------
                // 1. LEFT STICKY COLUMN: Subject Names (Smaller 100.dp, fixed to screen)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF111827))
                ) {
                    GridCell(
                        text = subjectHeader,
                        isHeader = true,
                        alignStart = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(Color(0xFF1F2937))
                    )

                    HorizontalDivider(color = Color(0xFF374151))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = lazyListState
                    ) {
                        items(rows) { row ->
                            val subjectName = row.getOrNull(1) ?: ""
                            GridCell(
                                text = subjectName,
                                isHeader = false,
                                isSubjectName = true,
                                alignStart = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            )
                            HorizontalDivider(color = Color(0xFF1F2937))
                        }
                    }
                }

                VerticalDivider(color = Color(0xFF374151))

                // -------------------------------------------------------------
                // 2. HORIZONTALLY SCROLLABLE DATA GRID (Dates + Atted/Held & %)
                // Spans from 100.dp to the right edge of the screen!
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(horizontalScrollState)
                ) {
                    // Header Row: Date Headers followed by Summary Headers
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .background(Color(0xFF1F2937))
                    ) {
                        dateHeaders.forEach { headerText ->
                            GridCell(
                                text = headerText,
                                isHeader = true,
                                modifier = Modifier
                                    .width(62.dp)
                                    .fillMaxHeight()
                            )
                        }
                        summaryHeaders.forEach { hText ->
                            val w = if (hText.contains("%")) 65.dp else 85.dp
                            GridCell(
                                text = hText,
                                isHeader = true,
                                modifier = Modifier
                                    .width(w)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF374151))

                    // LazyColumn for Data Rows
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = lazyListState
                    ) {
                        items(rows) { row ->
                            val dataCells = if (row.size > 2) row.subList(2, row.size) else emptyList()
                            Row(modifier = Modifier.height(48.dp)) {
                                // 1. Date Cells
                                dateHeaders.forEachIndexed { dateIdx, _ ->
                                    val cellText = dataCells.getOrNull(dateIdx) ?: ""
                                    GridCell(
                                        text = cellText,
                                        isHeader = false,
                                        modifier = Modifier
                                            .width(62.dp)
                                            .fillMaxHeight()
                                    )
                                }
                                // 2. Summary Cells (Atted/Held & %)
                                summaryHeaderIndices.forEach { sumColIdx ->
                                    val hText = allDataHeaders.getOrNull(sumColIdx) ?: ""
                                    val cellText = dataCells.getOrNull(sumColIdx) ?: ""
                                    val w = if (hText.contains("%")) 65.dp else 85.dp
                                    GridCell(
                                        text = cellText,
                                        isHeader = false,
                                        modifier = Modifier
                                            .width(w)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFF1F2937))
                        }
                    }
                }
            }
        }
    }
}

private fun getColumnWidth(headerText: String): Dp {
    val clean = headerText.trim().lowercase()
    return when {
        clean.contains("held") || clean.contains("atted") -> 85.dp
        clean.contains("%") -> 60.dp
        else -> 60.dp
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
    val trimmed = text.trim()

    val (bgColor, textColor) = when {
        isHeader -> Pair(
            Color(0xFF1F2937),
            Color(0xFFF3F4F6)
        )
        isSubjectName -> Pair(
            Color.Transparent,
            Color(0xFFE5E7EB)
        )
        trimmed.startsWith("P") -> Pair(Color(0xFF143823), Color(0xFF00FF87))
        trimmed.startsWith("A") -> Pair(Color(0xFF3F1717), Color(0xFFFF5252))
        trimmed.startsWith("H") -> Pair(Color(0xFF17253F), Color(0xFF40C4FF))
        trimmed.startsWith("L") -> Pair(Color(0xFF2E173F), Color(0xFFE040FB))
        trimmed.toDoubleOrNull() != null -> {
            val pct = trimmed.toDouble()
            if (pct >= 75.0) {
                Pair(Color.Transparent, Color(0xFF00FF87))
            } else {
                Pair(Color.Transparent, Color(0xFFFF5252))
            }
        }
        else -> Pair(Color.Transparent, Color(0xFF9CA3AF))
    }

    Surface(
        modifier = modifier,
        color = bgColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.Center
        ) {
            Text(
                text = trimmed,
                fontWeight = if (isHeader || trimmed.toDoubleOrNull() != null || trimmed.startsWith("P") || trimmed.startsWith("A")) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (isSubjectName) 11.sp else 12.sp,
                color = textColor,
                textAlign = if (alignStart) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

