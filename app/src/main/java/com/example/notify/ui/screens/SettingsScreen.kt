package com.example.notify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.notify.data.ReminderStorage
import com.example.notify.ui.AppState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAutoClearDialog by remember { mutableStateOf(false) }

    var snoozeTemp by remember { mutableStateOf(ReminderStorage.getSnoozeMins(context)) }
    var themeTemp by remember { mutableStateOf(ReminderStorage.getTheme(context)) }
    var autoClearText by remember { mutableStateOf(ReminderStorage.getAutoClearHours(context).toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            ListItem(
                headlineContent = { Text("Snooze Duration") },
                supportingContent = { Text("${ReminderStorage.getSnoozeMins(context)} Minutes") },
                modifier = Modifier.clickable { showSnoozeDialog = true }
            )
            Divider()
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
            Divider()
            ListItem(
                headlineContent = { Text("Auto-Clear History") },
                supportingContent = {
                    val hrs = ReminderStorage.getAutoClearHours(context)
                    Text(if (hrs == 0) "Disabled (Do not save history)" else "Keep for $hrs hours")
                },
                modifier = Modifier.clickable { showAutoClearDialog = true }
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
