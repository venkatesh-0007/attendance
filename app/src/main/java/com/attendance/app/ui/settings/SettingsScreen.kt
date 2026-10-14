package com.attendance.app.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendance.app.data.model.UserAccount
import com.attendance.app.theme.*
import com.attendance.app.ui.dashboard.components.LiquidGlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onAddAccount: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val attendanceState by viewModel.attendanceStateFlow.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val accent = LocalAccentColor.current
    var editingAccountForName by remember { mutableStateOf<UserAccount?>(null) }

    editingAccountForName?.let { accountToEdit ->
        EditAccountNameDialog(
            account = accountToEdit,
            onDismiss = { editingAccountForName = null },
            onConfirm = { newCustomName ->
                viewModel.updateAccountCustomName(accountToEdit.studentId, newCustomName, context)
                editingAccountForName = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Preferences",
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
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                // 1. ACTIVE PROFILE HEADER
                item {
                    val activeAccount = viewModel.savedAccounts.find { it.studentId == viewModel.currentStudentId }
                    val studentName = activeAccount?.displayName ?: attendanceState?.studentName ?: "Account (${viewModel.currentStudentId ?: "N/A"})"
                    val rawRoll = attendanceState?.rollNumber ?: viewModel.currentStudentId ?: "N/A"
                    val rollNumber = rawRoll.trim().removePrefix(":").trim().uppercase()
                    val firstLetter = studentName.trim().take(1).uppercase()

                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderBrush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.25f), accent.copy(alpha = 0.25f), Color.White.copy(alpha = 0.05f))
                        ),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(accent.copy(alpha = 0.85f), accent)
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                    .shadow(elevation = 6.dp, shape = CircleShape, spotColor = accent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = firstLetter,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = studentName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = rollNumber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.refresh() },
                                enabled = !isRefreshing
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = accent)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = accent)
                                }
                            }
                        }
                    }
                }

                // 2. ACCENT COLOR CUSTOMIZATION (Prominently featured!)
                item {
                    SectionHeader("App Accent Theme")
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(
                            text = "Choose Your Accent Color",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dynamically customizes cards, glowing indicators, buttons and highlights",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Flow / Grid of Preset Color Tiles
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppAccentPresets.take(4).forEach { preset ->
                                    val isSelected = (viewModel.accentColor == preset.hex) || (viewModel.accentColor == null && preset.id == "blue")
                                    AccentPresetChip(
                                        preset = preset,
                                        isSelected = isSelected,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.updateAccentColor(preset.hex, context) }
                                    )
                                }
                            }

                            // Row 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AppAccentPresets.drop(4).forEach { preset ->
                                    val isSelected = viewModel.accentColor == preset.hex
                                    AccentPresetChip(
                                        preset = preset,
                                        isSelected = isSelected,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.updateAccentColor(preset.hex, context) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. MULTI-ACCOUNT MANAGEMENT
                item {
                    SectionHeader("Linked Student Accounts")
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            viewModel.savedAccounts.forEach { account ->
                                val isCurrent = account.studentId == viewModel.currentStudentId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isCurrent) accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                        .border(
                                            0.75.dp,
                                            if (isCurrent) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.switchAccount(account.studentId, context) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = account.studentId,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White.copy(alpha = 0.6f)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { editingAccountForName = account }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = Color.White.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                                        }
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(accent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        } else {
                                            IconButton(onClick = { viewModel.removeAccount(account.studentId, context) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MacCritical, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .clickable { onAddAccount() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                    Text("+ Add Another Student", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 4. ATTENDANCE TARGET THRESHOLD
                item {
                    SectionHeader("Attendance Goal")
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Threshold", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accent.copy(alpha = 0.16f))
                                    .border(0.75.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("${viewModel.notificationThreshold}%", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = accent)
                            }
                        }

                        Slider(
                            value = viewModel.notificationThreshold.toFloat(),
                            onValueChange = { viewModel.updateNotificationThreshold(it.toInt(), context) },
                            valueRange = 50f..95f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Used to compute Safe to Skip periods and highlight target alerts across the app.",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // 5. AUTO-SYNC SCHEDULE
                item {
                    SectionHeader("Background Sync")
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active Hours Sync Only", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Only fetch in background during college hours", fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                            Switch(
                                checked = viewModel.autoSyncActiveHoursOnly,
                                onCheckedChange = { viewModel.updateAutoSyncActiveHoursOnly(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = accent, checkedTrackColor = accent.copy(alpha = 0.35f))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Sync Frequency", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.85f))
                        Spacer(modifier = Modifier.height(6.dp))

                        val intervals = listOf(30 to "30m", 60 to "1h", 120 to "2h", 180 to "3h", 360 to "6h")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            intervals.forEach { (mins, label) ->
                                val isSelected = viewModel.refreshIntervalMinutes == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) accent else Color.White.copy(alpha = 0.06f))
                                        .border(0.75.dp, if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updateRefreshInterval(mins, context) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. SYSTEM MAINTENANCE
                item {
                    SectionHeader("Maintenance")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.clearCache() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Clear Cache", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MacCritical.copy(alpha = 0.16f))
                                .border(1.dp, MacCritical.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.logout(); onLogout() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Log Out All", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MacCritical)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        color = Color.White.copy(alpha = 0.5f),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun AccentPresetChip(
    preset: AppAccentColor,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) preset.color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
            .border(
                if (isSelected) 1.5.dp else 0.5.dp,
                if (isSelected) preset.color else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(preset.gradientStart, preset.gradientEnd)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .shadow(elevation = if (isSelected) 4.dp else 0.dp, shape = CircleShape, spotColor = preset.glowColor),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Text(
                text = preset.name.split(" ").last(),
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun EditAccountNameDialog(
    account: UserAccount,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var customName by remember { mutableStateOf(account.customName ?: "") }
    val accent = LocalAccentColor.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SpaceNavyDark,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Edit Account Name", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set a custom display nickname for student ID ${account.studentId}.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(customName) }) {
                Text("Save", color = accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.65f))
            }
        }
    )
}
