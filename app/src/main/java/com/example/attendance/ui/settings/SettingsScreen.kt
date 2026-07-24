package com.example.attendance.ui.settings

import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.attendance.data.local.SecurePreferences
import com.example.attendance.data.model.UserAccount
import com.example.attendance.data.repository.AttendanceRepository
import com.example.attendance.worker.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val currentStudentId: String?
        get() = prefs.studentId

    fun updateDarkMode(mode: String) {
        prefs.darkMode = mode
        darkMode = mode
    }

    fun updateAccentColor(hex: String?) {
        prefs.accentColor = hex
        accentColor = hex
    }

    fun updateNotificationThreshold(threshold: Int) {
        prefs.notificationThreshold = threshold
        notificationThreshold = threshold
    }

    fun updateRefreshInterval(minutes: Int, context: Context) {
        prefs.refreshIntervalMinutes = minutes
        refreshIntervalMinutes = minutes
        rescheduleWorker(context)
    }

    fun switchAccount(studentId: String) {
        repository.switchAccount(studentId)
    }

    fun removeAccount(studentId: String) {
        repository.removeAccount(studentId)
        savedAccounts = repository.getSavedAccounts()
    }

    fun logout() {
        repository.logout()
    }

    fun clearCache() {
        repository.clearCache()
    }

    private fun rescheduleWorker(context: Context) {
        val intervalMinutes = prefs.refreshIntervalMinutes.toLong()
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(intervalMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "attendance_sync_work",
            ExistingPeriodicWorkPolicy.UPDATE,
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
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                SectionHeader("Manage Accounts")
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
                                onSwitch = { viewModel.switchAccount(account.studentId) },
                                onRemove = { viewModel.removeAccount(account.studentId) }
                            )
                        }

                        TextButton(
                            onClick = onAddAccount,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Another Account", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

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
                            listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                                FilterChip(
                                    selected = viewModel.darkMode == mode,
                                    onClick = { viewModel.updateDarkMode(mode) },
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
                            colors.forEach { (hex, name) ->
                                val color = hex?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
                                ColorCircle(
                                    color = color,
                                    isSelected = viewModel.accentColor == hex,
                                    onClick = { viewModel.updateAccentColor(hex) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader("Sync & Alerts")
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
                        Text("Notification Threshold: ${viewModel.notificationThreshold}%", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = viewModel.notificationThreshold.toFloat(),
                            onValueChange = { viewModel.updateNotificationThreshold(it.toInt()) },
                            valueRange = 50f..95f
                        )
                    }
                }
            }

            item {
                SectionHeader("Maintenance")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.clearCache() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Clear Cache", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        onClick = { viewModel.logout(); onLogout() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Log Out All", style = MaterialTheme.typography.labelLarge)
                    }
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
        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
    )
}

@Composable
fun AccountItem(
    account: UserAccount,
    isCurrent: Boolean,
    onSwitch: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isCurrent) { onSwitch() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = account.studentName?.take(1) ?: "?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = account.studentName ?: "Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = account.studentId, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isCurrent) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            }
        }
    }
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
                if (isSelected) Modifier.padding(4.dp).background(Color.White.copy(alpha = 0.5f), CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
    }
}

@Composable
fun SyncIntervalSelector(currentInterval: Int, onIntervalSelected: (Int) -> Unit) {
    val options = listOf(60 to "1h", 180 to "3h", 360 to "6h", 720 to "12h", 1440 to "24h")
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Sync Every", style = MaterialTheme.typography.titleMedium)
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(options.find { it.first == currentInterval }?.second ?: "3h", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
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
