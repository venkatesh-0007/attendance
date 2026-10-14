package com.attendance.app.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.attendance.app.data.model.CleanTable
import com.attendance.app.theme.*

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

private val SubjectColumnWidth = 140.dp
private val DateColumnWidth = 62.dp
private val RowHeight = 44.dp
private val HeaderHeight = 40.dp

private val TableBgColor = SpaceNavyDeep
private val HeaderBgColor = SpaceNavyDark
private val SubjectColBgColor = SpaceNavyDark
private val BorderColor = Color.White.copy(alpha = 0.08f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceTableScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val attendanceState by viewModel.attendance.collectAsState()
    val threshold = viewModel.notificationThreshold
    var gridSearchText by remember { mutableStateOf("") }
    val accent = LocalAccentColor.current

    val tableModel by produceState<TableUiModel?>(initialValue = null, attendanceState, threshold) {
        value = withContext(Dispatchers.Default) {
            val clean = attendanceState?.getCleanTable() ?: return@withContext null
            buildTableUiModel(clean, threshold.toDouble())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Attendance Grid",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TableBgColor)
        ) {
            if (tableModel == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Loading attendance matrix...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else if (tableModel!!.rows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No attendance table records available",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                val currentModel = tableModel!!
                val filteredRows = remember(currentModel.rows, gridSearchText) {
                    if (gridSearchText.trim().isEmpty()) {
                        currentModel.rows
                    } else {
                        currentModel.rows.filter {
                            it.subjectName.contains(gridSearchText.trim(), ignoreCase = true)
                        }
                    }
                }

                // -------------------------------------------------------------
                // 1. MAC-STYLE CONTROL BAR (Search + Status Legend + Counter)
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpaceNavyDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Compact Search Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(15.dp)
                                )
                                BasicTextField(
                                    value = gridSearchText,
                                    onValueChange = { gridSearchText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    decorationBox = { innerTextField ->
                                        if (gridSearchText.isEmpty()) {
                                            Text(
                                                "Find subject...",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.4f)
                                            )
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (gridSearchText.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { gridSearchText = "" }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Filtered Subjects Count
                        Text(
                            text = "${filteredRows.size} of ${currentModel.rows.size} Subjects",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }

                    // Status Legend Row (Matching Mac GridTab)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GridLegendItem(code = "P", title = "Present", color = MacSafe)
                        GridLegendItem(code = "A", title = "Absent", color = MacCritical)
                        GridLegendItem(code = "H", title = "Holiday", color = MacCyan)
                        GridLegendItem(code = "L", title = "Leave", color = MacPurple)
                    }
                }

                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                // -------------------------------------------------------------
                // 2. FROZEN COLUMN REGISTER TABLE (Mac Styled)
                // -------------------------------------------------------------
                val verticalScrollState = rememberScrollState()
                val horizontalScrollState = rememberScrollState()

                // Top Sticky Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeaderHeight)
                        .background(HeaderBgColor)
                ) {
                    // Frozen Top-Left Subject Header
                    Box(
                        modifier = Modifier
                            .width(SubjectColumnWidth)
                            .fillMaxHeight()
                            .background(HeaderBgColor)
                            .border(0.5.dp, BorderColor)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentModel.subjectHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // Scrollable Date & Summary Headers
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        currentModel.dateHeaders.forEach { headerText ->
                            Box(
                                modifier = Modifier
                                    .width(DateColumnWidth)
                                    .fillMaxHeight()
                                    .background(HeaderBgColor)
                                    .border(0.5.dp, BorderColor)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = headerText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        currentModel.summaryHeaders.forEach { summaryHeader ->
                            Box(
                                modifier = Modifier
                                    .width(summaryHeader.width)
                                    .fillMaxHeight()
                                    .background(HeaderBgColor)
                                    .border(0.5.dp, BorderColor)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = summaryHeader.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Scrollable Table Body
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                ) {
                    // Pinned Left Subject Column (Elevated over scrollable data)
                    Column(
                        modifier = Modifier
                            .width(SubjectColumnWidth)
                            .shadow(elevation = 4.dp, spotColor = Color.Black.copy(alpha = 0.5f))
                            .background(SubjectColBgColor)
                            .zIndex(2f)
                    ) {
                        filteredRows.forEachIndexed { index, row ->
                            val isEven = index % 2 == 0
                            Box(
                                modifier = Modifier
                                    .width(SubjectColumnWidth)
                                    .height(RowHeight)
                                    .background(if (isEven) SpaceNavyDark else SpaceNavyMid.copy(alpha = 0.4f))
                                    .border(0.5.dp, BorderColor)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = row.subjectName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.95f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Horizontally Scrollable Data Cells
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScrollState)
                            .zIndex(1f)
                    ) {
                        Column {
                            filteredRows.forEachIndexed { index, row ->
                                val isEven = index % 2 == 0
                                Row(
                                    modifier = Modifier
                                        .height(RowHeight)
                                        .background(if (isEven) SpaceNavyDeep else SpaceNavyDark.copy(alpha = 0.4f))
                                ) {
                                    row.dateCells.forEach { cell ->
                                        MacGridCellBox(cell)
                                    }
                                    row.summaryCells.forEach { cell ->
                                        MacGridCellBox(cell)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Mac Grid Cell View (Flattened single layout node)
private val CellCapsuleShape = RoundedCornerShape(6.dp)

@Composable
private fun MacGridCellBox(cell: CellUiModel) {
    val hasBackground = cell.bgColor != Color.Transparent
    Box(
        modifier = Modifier
            .width(cell.width)
            .fillMaxHeight()
            .border(0.5.dp, BorderColor)
            .padding(2.5.dp)
            .then(
                if (hasBackground) {
                    Modifier
                        .clip(CellCapsuleShape)
                        .background(cell.bgColor)
                        .border(0.5.dp, cell.textColor.copy(alpha = 0.25f), CellCapsuleShape)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = cell.text,
            fontSize = 11.sp,
            fontWeight = if (cell.isBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            color = cell.textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// MARK: - Mac Status Legend Item
@Composable
private fun GridLegendItem(code: String, title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
                .border(0.5.dp, color.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.65f)
        )
    }
}

private fun buildTableUiModel(cleanTable: CleanTable, targetThreshold: Double): TableUiModel {
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
        val width = if (hText.contains("%")) 64.dp else 74.dp
        SummaryHeaderUiModel(title = hText, width = width)
    }

    val rowUiModels = rows.map { row ->
        val subjectName = row.getOrNull(1) ?: ""
        val dataCells = if (row.size > 2) row.subList(2, row.size) else emptyList()

        val dateCellModels = dateHeaders.mapIndexed { dateIdx, _ ->
            val cellText = dataCells.getOrNull(dateIdx) ?: ""
            createMacCellUiModel(cellText, DateColumnWidth, targetThreshold)
        }

        val summaryCellModels = summaryHeaderIndices.map { sumColIdx ->
            val hText = allDataHeaders.getOrNull(sumColIdx) ?: ""
            val cellText = dataCells.getOrNull(sumColIdx) ?: ""
            val width = if (hText.contains("%")) 64.dp else 74.dp
            createMacCellUiModel(cellText, width, targetThreshold)
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

private fun createMacCellUiModel(rawText: String, width: Dp, targetThreshold: Double): CellUiModel {
    val trimmed = rawText.trim()
    val cleanNum = trimmed.replace("%", "").trim().toDoubleOrNull()

    val (bgColor, textColor, isBold) = when {
        trimmed.startsWith("P", ignoreCase = true) -> Triple(MacSafe.copy(alpha = 0.16f), MacSafe, true)
        trimmed.startsWith("A", ignoreCase = true) -> Triple(MacCritical.copy(alpha = 0.16f), MacCritical, true)
        trimmed.startsWith("H", ignoreCase = true) -> Triple(MacCyan.copy(alpha = 0.14f), MacCyan, false)
        trimmed.startsWith("L", ignoreCase = true) -> Triple(MacPurple.copy(alpha = 0.14f), MacPurple, false)
        cleanNum != null -> {
            val color = if (cleanNum >= targetThreshold) MacSafe else MacCritical
            Triple(color.copy(alpha = 0.12f), color, true)
        }
        else -> Triple(Color.Transparent, Color.White.copy(alpha = 0.45f), false)
    }

    return CellUiModel(
        text = trimmed,
        bgColor = bgColor,
        textColor = textColor,
        isBold = isBold,
        width = width
    )
}
