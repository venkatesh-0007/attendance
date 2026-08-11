package com.example.attendance.ui.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.UserAccount
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AttendanceRepository,
    private val prefs: SecurePreferences
) : ViewModel() {

    var darkMode by mutableStateOf(prefs.darkMode)
        private set

    var accentColor by mutableStateOf(prefs.accentColor)
        private set

    var notificationThreshold by mutableStateOf(prefs.notificationThreshold)
        private set

    var refreshIntervalMinutes by mutableStateOf(prefs.refreshIntervalMinutes)
        private set

    var savedAccounts by mutableStateOf(repository.getSavedAccounts())
        private set

    var currentStudentId by mutableStateOf(prefs.studentId)
        private set

    val attendanceStateFlow = repository.attendance

    var isRefreshing by mutableStateOf(false)
        private set

    val lastUpdated: Long
        get() = prefs.lastUpdated

    fun updateDarkMode(mode: String, context: Context) {
        prefs.darkMode = mode
        darkMode = mode
        refreshWidgets(context)
    }

    fun updateAccentColor(hex: String?, context: Context) {
        prefs.accentColor = hex
        accentColor = hex
        refreshWidgets(context)
    }

    fun updateNotificationThreshold(threshold: Int, context: Context) {
        prefs.notificationThreshold = threshold
        notificationThreshold = threshold
        refreshWidgets(context)
    }

    fun updateRefreshInterval(minutes: Int, context: Context) {
        prefs.refreshIntervalMinutes = minutes
        refreshIntervalMinutes = minutes
        rescheduleWorker(context)
    }

    private fun refreshWidgets(context: Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.example.attendance.widget.WidgetUpdater.updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun switchAccount(studentId: String, context: Context) {
        repository.switchAccount(studentId)
        currentStudentId = prefs.studentId
        savedAccounts = repository.getSavedAccounts()
        refreshWidgets(context)
    }

    fun removeAccount(studentId: String, context: Context) {
        repository.removeAccount(studentId)
        savedAccounts = repository.getSavedAccounts()
        currentStudentId = prefs.studentId
        refreshWidgets(context)
    }

    fun updateAccountCustomName(studentId: String, customName: String, context: Context) {
        repository.updateAccountCustomName(studentId, customName)
        savedAccounts = repository.getSavedAccounts()
        refreshWidgets(context)
    }

    fun logout() {
        repository.logout()
    }

    fun clearCache() {
        repository.clearCache()
    }

    fun refresh() {
        val studentId = prefs.studentId ?: return
        val password = prefs.password ?: return

        viewModelScope.launch {
            isRefreshing = true
            repository.fetchAttendance(studentId, password)
            isRefreshing = false
        }
    }

    private fun rescheduleWorker(context: Context) {
        val intervalMinutes = prefs.refreshIntervalMinutes.toLong().coerceAtLeast(15L)
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "attendance_sync_work",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            syncRequest
        )
    }
}

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
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. ACTIVE PROFILE HEADER
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val activeAccount = viewModel.savedAccounts.find { it.studentId == viewModel.currentStudentId }
                val studentName = activeAccount?.displayName ?: attendanceState?.student_name ?: "Account (${viewModel.currentStudentId ?: "N/A"})"
                val rawRoll = attendanceState?.roll_number ?: viewModel.currentStudentId ?: "N/A"
                val rollNumber = rawRoll.trim().removePrefix(":").trim()
                val firstLetter = studentName.trim().take(1).uppercase()

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstLetter,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = studentName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Roll No: $rollNumber",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. ACCOUNT MANAGEMENT
            item {
                SectionHeader("Account Management")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        viewModel.savedAccounts.forEach { account ->
                            val isCurrent = account.studentId == viewModel.currentStudentId
                            AccountItem(
                                account = account,
                                isCurrent = isCurrent,
                                onSwitch = { viewModel.switchAccount(account.studentId, context) },
                                onEditName = { editingAccountForName = account },
                                onRemove = { viewModel.removeAccount(account.studentId, context) }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        TextButton(
                            onClick = onAddAccount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Another Account", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // 3. ATTENDANCE CUSTOMIZATION
            item {
                SectionHeader("Attendance Customization")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "${viewModel.notificationThreshold}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Calculations across the app (like class skipping limits or additional classes needed) dynamically adapt to this target threshold.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Slider(
                            value = viewModel.notificationThreshold.toFloat(),
                            onValueChange = { viewModel.updateNotificationThreshold(it.toInt(), context) },
                            valueRange = 50f..95f
                        )
                    }
                }
            }

            // 4. DATA DIAGNOSTICS & SYNC
            item {
                SectionHeader("Synchronization & Diagnostics")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SyncIntervalSelector(
                            currentInterval = viewModel.refreshIntervalMinutes,
                            onIntervalSelected = { viewModel.updateRefreshInterval(it, context) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Last Sync Status Card
                        val lastSyncStr = if (viewModel.lastUpdated > 0) {
                            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(viewModel.lastUpdated))
                        } else {
                            "Never Synced"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (viewModel.lastUpdated > 0) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (viewModel.lastUpdated > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Status: Up-to-date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Last synced: $lastSyncStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.refresh() },
                            enabled = !viewModel.isRefreshing,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (viewModel.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync Now", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // 5. APPEARANCE Settings
            item {
                SectionHeader("Appearance")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "SYSTEM" to Icons.Default.SettingsSystemDaydream,
                                "LIGHT" to Icons.Default.LightMode,
                                "DARK" to Icons.Default.DarkMode
                            ).forEach { (mode, icon) ->
                                FilterChip(
                                    selected = viewModel.darkMode == mode,
                                    onClick = { viewModel.updateDarkMode(mode, context) },
                                    leadingIcon = {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                    label = { Text(mode, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Accent Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val colors = listOf(
                            null to "Default",
                            "#2196F3" to "Blue",
                            "#4CAF50" to "Green",
                            "#FF9800" to "Orange",
                            "#E91E63" to "Pink",
                            "#9C27B0" to "Purple"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            colors.forEach { (hex, _) ->
                                val color = hex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                                ColorCircle(
                                    color = color,
                                    isSelected = viewModel.accentColor == hex,
                                    onClick = { viewModel.updateAccentColor(hex, context) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. SYSTEM MAINTENANCE
            item {
                SectionHeader("Maintenance")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.clearCache() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Clear Cache", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { viewModel.logout(); onLogout() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Log Out All", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // 7. FOOTER
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Attendance Assistant v0.56",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Designed & Structured with Antigravity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun AccountItem(
    account: UserAccount,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onEditName: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrent) { onSwitch() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val initial = account.displayName.take(1).uppercase()
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initial, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Roll No: ${account.studentId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEditName) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit Account Name",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        if (isCurrent) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Active Account", tint = MaterialTheme.colorScheme.primary)
        } else {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Account", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun EditAccountNameDialog(
    account: UserAccount,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nameText by remember { mutableStateOf(account.customName ?: account.studentName ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Account / Roll No", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Set a custom name/nickname for Roll No: ${account.studentId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Account Name / Alias") },
                    placeholder = { Text("e.g. Venkat, Primary Account") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(nameText)
                    onDismiss()
                }
            ) {
                Text("Save Name")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier
                    .padding(4.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
    }
}

@Composable
fun SyncIntervalSelector(currentInterval: Int, onIntervalSelected: (Int) -> Unit) {
    val options = listOf(60 to "1 hour", 180 to "3 hours", 360 to "6 hours", 720 to "12 hours", 1440 to "24 hours")
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Background Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(
                    text = options.find { it.first == currentInterval }?.second ?: "3 hours",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (mins, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { onIntervalSelected(mins); expanded = false })
                }
            }
        }
    }
}
