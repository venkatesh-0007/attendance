package com.example.attendance.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        if (table == null || table.rows.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("No attendance table data available.")
            }
        } else {
            val horizontalScrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .horizontalScroll(horizontalScrollState)
            ) {
                // Header Row
                table.headers?.let { headers ->
                    Row(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                        headers.forEach { header ->
                            TableCell(text = header, isHeader = true)
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(table.rows) { row ->
                        Row {
                            row.forEach { cell ->
                                TableCell(text = cell)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(text: String, isHeader: Boolean = false) {
    val weight = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    
    // Status specific coloring
    val bgColor = when {
        text.trim() == "P" -> Color(0xFFE8F5E9) // Light Green
        text.trim() == "A" -> Color(0xFFFFEBEE) // Light Red
        else -> Color.Transparent
    }

    Surface(
        modifier = Modifier
            .width(80.dp)
            .height(48.dp),
        color = bgColor,
        border = if (!isHeader) null else null
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = weight,
                fontSize = 12.sp,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
