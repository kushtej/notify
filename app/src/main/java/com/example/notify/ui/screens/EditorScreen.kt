package com.example.notify.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notify.data.ReminderStorage
import com.example.notify.model.Reminder
import com.example.notify.scheduler.AlarmScheduler
import com.example.notify.ui.AppState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val existing = AppState.editingId?.let { id ->
        ReminderStorage.getAllReminders(context).find { it.id == id }
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var desc by remember { mutableStateOf(existing?.desc ?: "") }
    var frequency by remember { mutableStateOf(existing?.frequency ?: "One-Time") }
    var customDays by remember { mutableStateOf(existing?.customDays?.toSet() ?: setOf()) }

    val cal = Calendar.getInstance().apply {
        if (existing != null) timeInMillis = existing.timeInMillis
    }
    var selectedDateMillis by remember { mutableStateOf(cal.timeInMillis) }
    var selectedHour by remember { mutableStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(cal.get(Calendar.MINUTE)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedFreq by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val timePickerState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Reminder" else "Edit Reminder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        saveReminder(
                            context = context,
                            existing = existing,
                            title = title,
                            desc = desc,
                            frequency = frequency,
                            customDays = customDays,
                            selectedDateMillis = selectedDateMillis,
                            selectedHour = selectedHour,
                            selectedMinute = selectedMinute,
                            onBack = onBack
                        )
                    }) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            com.example.notify.ui.richtext.RichTextEditor(
                value = desc,
                onValueChange = { desc = it },
                modifier = Modifier.fillMaxWidth()
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Time picker card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        String.format("Time: %02d:%02d", selectedHour, selectedMinute),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Frequency dropdown
            FrequencyDropdown(
                frequency = frequency,
                expanded = expandedFreq,
                onExpandedChange = { expandedFreq = it },
                onFrequencySelected = { frequency = it; expandedFreq = false }
            )

            // Conditional date/day pickers
            when (frequency) {
                "One-Time" -> DatePickerCard(
                    dateMillis = selectedDateMillis,
                    formatter = dateFormatter,
                    onClick = { showDatePicker = true }
                )
                "Specific Days" -> DaySelector(
                    selectedDays = customDays,
                    onToggle = { day ->
                        customDays = if (customDays.contains(day)) customDays - day else customDays + day
                    }
                )
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyDropdown(
    frequency: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onFrequencySelected: (String) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) }
    ) {
        OutlinedTextField(
            value = frequency,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repeat") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            listOf("One-Time", "Daily", "Specific Days").forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onFrequencySelected(option) }
                )
            }
        }
    }
}

@Composable
private fun DatePickerCard(
    dateMillis: Long,
    formatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Date: ${formatter.format(Date(dateMillis))}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun DaySelector(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val days = listOf(
        "S" to Calendar.SUNDAY,
        "M" to Calendar.MONDAY,
        "T" to Calendar.TUESDAY,
        "W" to Calendar.WEDNESDAY,
        "T" to Calendar.THURSDAY,
        "F" to Calendar.FRIDAY,
        "S" to Calendar.SATURDAY
    )

    Text("Select Days:", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (label, value) ->
            val isSelected = selectedDays.contains(value)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onToggle(value) }
            ) {
                Text(
                    label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Validates and saves a reminder, then navigates back.
 */
private fun saveReminder(
    context: android.content.Context,
    existing: Reminder?,
    title: String,
    desc: String,
    frequency: String,
    customDays: Set<Int>,
    selectedDateMillis: Long,
    selectedHour: Int,
    selectedMinute: Int,
    onBack: () -> Unit
) {
    if (title.isBlank()) {
        Toast.makeText(context, "Title required", Toast.LENGTH_SHORT).show()
        return
    }

    val triggerCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, selectedHour)
        set(Calendar.MINUTE, selectedMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    when (frequency) {
        "One-Time" -> {
            triggerCal.timeInMillis = selectedDateMillis
            triggerCal.set(Calendar.HOUR_OF_DAY, selectedHour)
            triggerCal.set(Calendar.MINUTE, selectedMinute)
            triggerCal.set(Calendar.SECOND, 0)
            triggerCal.set(Calendar.MILLISECOND, 0)
        }
        "Specific Days" -> {
            if (customDays.isEmpty()) {
                Toast.makeText(context, "Select at least one day", Toast.LENGTH_SHORT).show()
                return
            }
            triggerCal.timeInMillis = AlarmScheduler.getNextDayMillis(selectedHour, selectedMinute, customDays)
        }
        else -> {
            if (triggerCal.timeInMillis <= System.currentTimeMillis()) {
                triggerCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    if (triggerCal.timeInMillis <= System.currentTimeMillis() && frequency == "One-Time") {
        Toast.makeText(context, "Time must be in the future", Toast.LENGTH_SHORT).show()
        return
    }

    val reminder = Reminder(
        id = existing?.id ?: System.currentTimeMillis().toInt(),
        title = title,
        desc = desc,
        timeInMillis = triggerCal.timeInMillis,
        frequency = frequency,
        customDays = customDays.toList()
    )

    ReminderStorage.saveReminder(context, reminder)
    AlarmScheduler.schedule(context, reminder)
    AppState.forceRefresh += 1
    onBack()
}
