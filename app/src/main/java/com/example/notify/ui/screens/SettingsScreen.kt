package com.example.notify.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.notify.data.ReminderStorage
import com.example.notify.model.FilterConfig
import com.example.notify.ui.AppState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsSubPage {
    DashboardFilters,
    BackupRestore
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    var currentSubPage by remember { mutableStateOf<SettingsSubPage?>(null) }

    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoClearDialog by remember { mutableStateOf(false) }

    var snoozeTemp by remember { mutableStateOf(ReminderStorage.getSnoozeMins(context)) }
    var themeTemp by remember { mutableStateOf(ReminderStorage.getTheme(context)) }
    var autoClearText by remember { mutableStateOf(ReminderStorage.getAutoClearHours(context).toString()) }

    BackHandler(enabled = currentSubPage != null) {
        currentSubPage = null
    }

    when (currentSubPage) {
        SettingsSubPage.DashboardFilters -> {
            DashboardFiltersSubPage(onBack = { currentSubPage = null })
        }

        SettingsSubPage.BackupRestore -> {
            BackupRestoreSubPage(onBack = { currentSubPage = null })
        }

        null -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Settings", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    ListItem(
                        headlineContent = { Text("Snooze Duration") },
                        supportingContent = { Text("${ReminderStorage.getSnoozeMins(context)} Minutes") },
                        modifier = Modifier.clickable { showSnoozeDialog = true }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("App Theme") },
                        supportingContent = {
                            Text(
                                when (ReminderStorage.getTheme(context)) {
                                    1 -> "Light"
                                    2 -> "Dark"
                                    else -> "System Default"
                                }
                            )
                        },
                        modifier = Modifier.clickable { showThemeDialog = true }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Auto-Clear History") },
                        supportingContent = {
                            val hrs = ReminderStorage.getAutoClearHours(context)
                            Text(if (hrs == 0) "Disabled (Do not save history)" else "Keep for $hrs hours")
                        },
                        modifier = Modifier.clickable { showAutoClearDialog = true }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Dashboard Filters") },
                        supportingContent = { Text("Customize and reorder filter chips") },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            currentSubPage = SettingsSubPage.DashboardFilters
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Backup & Restore") },
                        supportingContent = { Text("Save backups or restore notifications") },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            currentSubPage = SettingsSubPage.BackupRestore
                        }
                    )
                }
            }

            if (showSnoozeDialog) {
                SnoozeDurationDialog(
                    selectedMins = snoozeTemp,
                    onSelect = { snoozeTemp = it },
                    onConfirm = {
                        ReminderStorage.setSnoozeMins(context, snoozeTemp)
                        showSnoozeDialog = false
                    },
                    onDismiss = { showSnoozeDialog = false }
                )
            }

            if (showThemeDialog) {
                ThemeDialog(
                    selectedTheme = themeTemp,
                    onSelect = { themeTemp = it },
                    onConfirm = {
                        ReminderStorage.setTheme(context, themeTemp)
                        AppState.themeRefresh += 1
                        showThemeDialog = false
                    },
                    onDismiss = { showThemeDialog = false }
                )
            }

            if (showAutoClearDialog) {
                AutoClearDialog(
                    text = autoClearText,
                    onTextChange = { autoClearText = it.filter { char -> char.isDigit() } },
                    onConfirm = {
                        val hrs = autoClearText.toIntOrNull() ?: 0
                        ReminderStorage.setAutoClearHours(context, hrs)
                        ReminderStorage.pruneOldCompleted(context)
                        AppState.forceRefresh += 1
                        showAutoClearDialog = false
                    },
                    onDismiss = { showAutoClearDialog = false }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Dashboard Filters Sub-Page
// ════════════════════════════════════════════════════════════════════

@Composable
private fun DragGripHandle(
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 24.dp)) {
        val dotRadius = 1.75.dp.toPx()
        val spacingY = 5.dp.toPx()
        val colSpacing = 6.dp.toPx()
        val startX = (size.width - colSpacing) / 2f
        val startY = (size.height - spacingY * 2) / 2f

        for (col in 0..1) {
            val cx = startX + col * colSpacing
            for (row in 0..2) {
                val cy = startY + row * spacingY
                drawCircle(
                    color = tint,
                    radius = dotRadius,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardFiltersSubPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var filterConfigs by remember {
        mutableStateOf(ReminderStorage.getFilterConfigs(context))
    }

    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Filters", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filterConfigs, key = { it.id }) { item ->
                    val isDragging = draggedItemId == item.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 10f else 1f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                            }
                            .onGloballyPositioned { coordinates ->
                                if (itemHeightPx == 0f) {
                                    itemHeightPx = coordinates.size.height.toFloat()
                                }
                            },
                        color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        shadowElevation = if (isDragging) 8.dp else 0.dp,
                        tonalElevation = if (isDragging) 4.dp else 0.dp,
                        shape = if (isDragging) RoundedCornerShape(12.dp) else RectangleShape
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(item.label, fontWeight = FontWeight.Medium)
                            },
                            supportingContent = {
                                Text(item.description)
                            },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .pointerInput(item.id) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedItemId = item.id
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    val currentId = draggedItemId ?: return@detectDragGestures
                                                    var currentIdx = filterConfigs.indexOfFirst { it.id == currentId }
                                                    if (currentIdx == -1) return@detectDragGestures
                                                    val itemHeight = if (itemHeightPx > 0f) itemHeightPx else 180f
                                                    val threshold = itemHeight * 0.5f

                                                    while (dragOffsetY > threshold && currentIdx < filterConfigs.size - 1) {
                                                        val nextIdx = currentIdx + 1
                                                        val mutable = filterConfigs.toMutableList()
                                                        val moved = mutable.removeAt(currentIdx)
                                                        mutable.add(nextIdx, moved)
                                                        filterConfigs = mutable
                                                        dragOffsetY -= itemHeight
                                                        currentIdx = nextIdx
                                                    }

                                                    while (dragOffsetY < -threshold && currentIdx > 0) {
                                                        val prevIdx = currentIdx - 1
                                                        val mutable = filterConfigs.toMutableList()
                                                        val moved = mutable.removeAt(currentIdx)
                                                        mutable.add(prevIdx, moved)
                                                        filterConfigs = mutable
                                                        dragOffsetY += itemHeight
                                                        currentIdx = prevIdx
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedItemId = null
                                                    dragOffsetY = 0f
                                                    ReminderStorage.setFilterConfigs(context, filterConfigs)
                                                    AppState.forceRefresh += 1
                                                },
                                                onDragCancel = {
                                                    draggedItemId = null
                                                    dragOffsetY = 0f
                                                    ReminderStorage.setFilterConfigs(context, filterConfigs)
                                                    AppState.forceRefresh += 1
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    DragGripHandle(
                                        tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = item.enabled,
                                    onCheckedChange = { checked ->
                                        val mutable = filterConfigs.toMutableList()
                                        val idx = mutable.indexOfFirst { it.id == item.id }
                                        if (idx != -1) {
                                            mutable[idx] = item.copy(enabled = checked)
                                            filterConfigs = mutable
                                            ReminderStorage.setFilterConfigs(context, mutable)
                                            AppState.forceRefresh += 1
                                        }
                                    }
                                )
                            }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Backup & Restore Sub-Page
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupRestoreSubPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var pendingImportCount by remember { mutableStateOf(0) }
    var restoreMode by remember { mutableStateOf("merge") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val data = ReminderStorage.exportRemindersJson(context)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(data.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Backup saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val data = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!data.isNullOrBlank()) {
                    val count = ReminderStorage.countRemindersInJson(data)
                    if (count > 0) {
                        pendingImportContent = data
                        pendingImportCount = count
                        restoreMode = "merge"
                        showRestoreConfirmDialog = true
                    } else {
                        Toast.makeText(context, "No valid notifications found in backup file", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Backups are saved offline to your device or cloud storage. You can restore them anytime or transfer your notifications to another phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ListItem(
                headlineContent = { Text("Create Backup") },
                supportingContent = { Text("Save a backup of all notifications") },
                modifier = Modifier.clickable {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    createBackupLauncher.launch("notify_backup_$timestamp.json")
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Restore from Backup") },
                supportingContent = { Text("Restore notifications from a backup file") },
                modifier = Modifier.clickable {
                    restoreBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            )
        }
    }

    if (showRestoreConfirmDialog && pendingImportContent != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirmDialog = false
                pendingImportContent = null
            },
            title = { Text("Restore Notifications") },
            text = {
                Column {
                    Text(
                        "Found $pendingImportCount notification(s) in this backup file.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select restore method:", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreMode = "merge" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = restoreMode == "merge", onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Merge", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Keep existing notifications and add new ones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { restoreMode = "replace" }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = restoreMode == "replace", onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Replace All", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Overwrite all existing notifications with backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val count = ReminderStorage.importRemindersFromJson(
                        context,
                        pendingImportContent!!,
                        replaceExisting = (restoreMode == "replace")
                    )
                    AppState.forceRefresh += 1
                    showRestoreConfirmDialog = false
                    pendingImportContent = null
                    val actionWord = if (restoreMode == "replace") "Replaced with" else "Restored"
                    Toast.makeText(context, "$actionWord $count notification(s)", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreConfirmDialog = false
                    pendingImportContent = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Dialogs
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SnoozeDurationDialog(
    selectedMins: Int,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snooze Duration") },
        text = {
            Column {
                listOf(5, 10, 15, 30, 60).forEach { opt ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(opt) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = selectedMins == opt, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("$opt Minutes")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        }
    )
}

@Composable
private fun ThemeDialog(
    selectedTheme: Int,
    onSelect: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Theme") },
        text = {
            Column {
                listOf(0 to "System Default", 1 to "Light", 2 to "Dark").forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = selectedTheme == value, onClick = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        }
    )
}

@Composable
private fun AutoClearDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Clear History") },
        text = {
            Column {
                Text("Time to keep completed reminders:", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Hours (0 to disable history)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
