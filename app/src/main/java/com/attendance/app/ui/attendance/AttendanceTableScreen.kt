package com.attendance.app.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.attendance.app.data.model.CleanTable

// --- Pre-computed, Immutable UI Models for Zero-Jank Rendering ---

@Immutable
private data class CellUiModel(
    val text: String,
    val bgColor: Color,
    val textColor: Color,
    val isBold: Boolean,
    val width: Dp
)

@Immutable
private data class SummaryHeaderUiModel(
    val title: String,
    val width: Dp
)

@Immutable
private data class TableRowUiModel(
    val subjectName: String,
    val dateCells: List<CellUiModel>,
    val summaryCells: List<CellUiModel>
)

@Immutable
private data class TableUiModel(
    val subjectHeader: String,
    val dateHeaders: List<String>,
    val summaryHeaders: List<SummaryHeaderUiModel>,
    val rows: List<TableRowUiModel>
)

private val SubjectColumnWidth = 104.dp
private val DateColumnWidth = 62.dp
private val RowHeight = 48.dp

private val TableBgColor = Color(0xFF0B0F19)
private val HeaderBgColor = Color(0xFF1F2937)
private val SubjectColBgColor = Color(0xFF111827)
private val HeaderDividerColor = Color(0xFF374151)
private val RowDividerColor = Color(0xFF1F2937)
private val HeaderTextColor = Color(0xFFF3F4F6)
private val SubjectTextColor = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceTableScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val attendanceState by viewModel.attendance.collectAsState()

    // Memoize the table cleaning step so regex/filters run only when data changes
    val cleanTable = remember(attendanceState) {
        attendanceState?.getCleanTable()
    }

    // Pre-calculate all cell styles, colors, and layout metrics into an immutable model
    val tableModel = remember(cleanTable) {
        cleanTable?.let { buildTableUiModel(it) }
    }

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
        if (tableModel == null || tableModel.rows.isEmpty()) {
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
            val verticalScrollState = rememberScrollState()
            val horizontalScrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(TableBgColor)
            ) {
                // -------------------------------------------------------------
                // 1. TOP STICKY HEADER ROW (Subject Header + Date/Summary Headers)
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RowHeight)
                        .background(HeaderBgColor)
                ) {
                    // Pinned top-left corner (Subject header)
                    Box(
                        modifier = Modifier
                            .width(SubjectColumnWidth)
                            .fillMaxHeight()
                            .background(HeaderBgColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = tableModel.subjectHeader,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = HeaderTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    VerticalDivider(color = HeaderDividerColor)

                    // Horizontally scrollable header data
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        tableModel.dateHeaders.forEach { headerText ->
                            Box(
                                modifier = Modifier
                                    .width(DateColumnWidth)
                                    .fillMaxHeight()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = headerText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = HeaderTextColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        tableModel.summaryHeaders.forEach { summaryHeader ->
                            Box(
                                modifier = Modifier
                                    .width(summaryHeader.width)
                                    .fillMaxHeight()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = summaryHeader.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = HeaderTextColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = HeaderDividerColor)

                // -------------------------------------------------------------
                // 2. SCROLLABLE BODY (Unified vertical scroll container)
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                ) {
                    // Pinned Left Column: Subject Names
                    Column(
                        modifier = Modifier
                            .width(SubjectColumnWidth)
                            .background(SubjectColBgColor)
                    ) {
                        tableModel.rows.forEach { row ->
                            Box(
                                modifier = Modifier
                                    .width(SubjectColumnWidth)
                                    .height(RowHeight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = row.subjectName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = SubjectTextColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp
                                )
                            }
                            HorizontalDivider(color = RowDividerColor)
                        }
                    }

                    VerticalDivider(color = HeaderDividerColor)

                    // Right Data Grid: Attendance Data
                    // Exactly ONE horizontalScroll modifier for all data rows
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column {
                            tableModel.rows.forEach { row ->
                                Row(
                                    modifier = Modifier.height(RowHeight)
                                ) {
                                    row.dateCells.forEach { cell ->
                                        GridCellBox(cell)
                                    }
                                    row.summaryCells.forEach { cell ->
                                        GridCellBox(cell)
                                    }
                                }
                                HorizontalDivider(color = RowDividerColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCellBox(cell: CellUiModel) {
    Box(
        modifier = Modifier
            .width(cell.width)
            .fillMaxHeight()
            .background(cell.bgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.text,
            fontWeight = if (cell.isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            color = cell.textColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildTableUiModel(cleanTable: CleanTable): TableUiModel {
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
    } else {
        allDataHeaders
    }

    val summaryHeaders = summaryHeaderIndices.map { sumColIdx ->
        val hText = allDataHeaders.getOrNull(sumColIdx) ?: ""
        val width = if (hText.contains("%")) 65.dp else 85.dp
        SummaryHeaderUiModel(title = hText, width = width)
    }

    val rowUiModels = rows.map { row ->
        val subjectName = row.getOrNull(1) ?: ""
        val dataCells = if (row.size > 2) row.subList(2, row.size) else emptyList()

        val dateCellModels = dateHeaders.mapIndexed { dateIdx, _ ->
            val cellText = dataCells.getOrNull(dateIdx) ?: ""
            createCellUiModel(cellText, DateColumnWidth)
        }

        val summaryCellModels = summaryHeaderIndices.map { sumColIdx ->
            val hText = allDataHeaders.getOrNull(sumColIdx) ?: ""
            val cellText = dataCells.getOrNull(sumColIdx) ?: ""
            val width = if (hText.contains("%")) 65.dp else 85.dp
            createCellUiModel(cellText, width)
        }

        TableRowUiModel(
            subjectName = subjectName,
            dateCells = dateCellModels,
            summaryCells = summaryCellModels
        )
    }

    return TableUiModel(
        subjectHeader = subjectHeader,
        dateHeaders = dateHeaders,
        summaryHeaders = summaryHeaders,
        rows = rowUiModels
    )
}

private fun createCellUiModel(rawText: String, width: Dp): CellUiModel {
    val trimmed = rawText.trim()
    val cleanNum = trimmed.replace("%", "").trim().toDoubleOrNull()

    val (bgColor, textColor, isBold) = when {
        trimmed.startsWith("P") -> Triple(Color(0xFF143823), Color(0xFF00FF87), true)
        trimmed.startsWith("A") -> Triple(Color(0xFF3F1717), Color(0xFFFF5252), true)
        trimmed.startsWith("H") -> Triple(Color(0xFF17253F), Color(0xFF40C4FF), false)
        trimmed.startsWith("L") -> Triple(Color(0xFF2E173F), Color(0xFFE040FB), false)
        cleanNum != null -> {
            val color = if (cleanNum >= 75.0) Color(0xFF00FF87) else Color(0xFFFF5252)
            Triple(Color.Transparent, color, true)
        }
        else -> Triple(Color.Transparent, Color(0xFF9CA3AF), false)
    }

    return CellUiModel(
        text = trimmed,
        bgColor = bgColor,
        textColor = textColor,
        isBold = isBold,
        width = width
    )
}
