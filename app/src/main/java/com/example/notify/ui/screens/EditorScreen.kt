package com.example.notify.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notify.data.ReminderStorage
import com.example.notify.model.Reminder
import com.example.notify.scheduler.AlarmScheduler
import com.example.notify.ui.AppState
import com.example.notify.ui.FrequencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val existing = AppState.editingId?.let { id ->
        ReminderStorage.getAllReminders(context).find { it.id == id }
    }

    var title by remember { mutableStateOf(existing?.title ?: "") }
    var desc by remember { mutableStateOf(existing?.desc ?: "") }
    var frequency by remember {
        mutableStateOf(
            when (existing?.frequency) {
                "Weekly", "Daily" -> "Specific Days"
                null -> "One-Time"
                else -> existing.frequency
            }
        )
    }

    // Days selection (SUNDAY = 1 .. SATURDAY = 7)
    val all7Days = remember {
        setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
    }
    var customDays by remember {
        mutableStateOf(
            when {
                existing != null && existing.customDays.isNotEmpty() -> existing.customDays.toSet()
                existing?.frequency == "Daily" -> all7Days
                else -> setOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
            }
        )
    }

    // Weekly/Bi-weekly interval
    var intervalWeeks by remember { mutableIntStateOf(existing?.intervalWeeks ?: 1) }

    // Monthly settings
    var monthlyType by remember { mutableStateOf(existing?.monthlyType ?: "day_of_month") }
    var monthlyDay by remember { mutableIntStateOf(existing?.monthlyDay ?: 1) }
    var monthlyWeekOrdinal by remember { mutableIntStateOf(existing?.monthlyWeekOrdinal ?: 1) }
    var monthlyDayOfWeek by remember { mutableIntStateOf(existing?.monthlyDayOfWeek ?: Calendar.TUESDAY) }
    var intervalMonths by remember { mutableIntStateOf(existing?.intervalMonths ?: 1) }

    // Notification times: list of "HH:mm". For a new reminder, starts empty (no default current time).
    var notificationTimes by remember {
        mutableStateOf(
            if (existing != null) {
                val c = Calendar.getInstance().apply { timeInMillis = existing.timeInMillis }
                val primary = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
                listOf(primary) + existing.extraTimes
            } else {
                emptyList()
            }
        )
    }

    val cal = Calendar.getInstance().apply {
        if (existing != null) timeInMillis = existing.timeInMillis
    }
    var selectedDateMillis by remember { mutableStateOf(cal.timeInMillis) }

    val primaryHour = if (notificationTimes.isNotEmpty()) {
        notificationTimes.first().split(":").getOrNull(0)?.toIntOrNull() ?: -1
    } else -1

    val primaryMinute = if (notificationTimes.isNotEmpty()) {
        notificationTimes.first().split(":").getOrNull(1)?.toIntOrNull() ?: -1
    } else -1

    val extraTimes = if (notificationTimes.size > 1) {
        notificationTimes.drop(1)
    } else emptyList()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableIntStateOf(-1) } // -1 = adding new time, >=0 = index of time being edited
    var expandedFreq by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Reminder" else "Edit Reminder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            notificationTimes = notificationTimes,
                            intervalDays = 1,
                            intervalWeeks = intervalWeeks,
                            monthlyType = monthlyType,
                            monthlyDay = monthlyDay,
                            monthlyWeekOrdinal = monthlyWeekOrdinal,
                            monthlyDayOfWeek = monthlyDayOfWeek,
                            intervalMonths = intervalMonths,
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
            // Subject & Description
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // ── Section 1: Notification Times Card ──────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Notification Times",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // All notification time chips
                        notificationTimes.forEachIndexed { index, timeStr ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    editingTimeIndex = index
                                    showTimePicker = true
                                },
                                label = { Text(formatTimeStr(timeStr), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                notificationTimes = notificationTimes.toMutableList().apply { removeAt(index) }
                                            }
                                    )
                                }
                            )
                        }

                        // Add time chip
                        AssistChip(
                            onClick = {
                                editingTimeIndex = -1
                                showTimePicker = true
                            },
                            label = { Text("+ Add Time") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // ── Section 2: Repeat Dropdown ──────────────────────────────
            FrequencyDropdown(
                frequency = frequency,
                expanded = expandedFreq,
                onExpandedChange = { expandedFreq = it },
                onFrequencySelected = { selected ->
                    frequency = selected
                    expandedFreq = false
                }
            )

            // ── Section 3: Frequency Configuration Card ─────────────────
            when (frequency) {
                "One-Time" -> {
                    DatePickerCard(
                        dateMillis = selectedDateMillis,
                        formatter = dateFormatter,
                        onClick = { showDatePicker = true },
                        primaryHour = primaryHour,
                        primaryMinute = primaryMinute,
                        extraTimes = extraTimes
                    )
                }

                "Daily", "Specific Days" -> {
                    DaysScheduleCard(
                        selectedDays = customDays,
                        onDaysChange = { customDays = it },
                        intervalWeeks = intervalWeeks,
                        onIntervalWeeksChange = { intervalWeeks = it },
                        primaryHour = primaryHour,
                        primaryMinute = primaryMinute,
                        extraTimes = extraTimes
                    )
                }

                "Monthly" -> {
                    MonthlyScheduleCard(
                        monthlyType = monthlyType,
                        onMonthlyTypeChange = { monthlyType = it },
                        monthlyDay = monthlyDay,
                        onMonthlyDayChange = { monthlyDay = it },
                        weekOrdinal = monthlyWeekOrdinal,
                        onWeekOrdinalChange = { monthlyWeekOrdinal = it },
                        dayOfWeek = monthlyDayOfWeek,
                        onDayOfWeekChange = { monthlyDayOfWeek = it },
                        intervalMonths = intervalMonths,
                        onIntervalMonthsChange = { intervalMonths = it },
                        primaryHour = primaryHour,
                        primaryMinute = primaryMinute,
                        extraTimes = extraTimes
                    )
                }
            }
        }
    }

    // ── Date picker dialog ─────────────────────────────────────────
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

    // ── Time picker dialog ─────────────────────────────────────────
    if (showTimePicker) {
        val initialH: Int
        val initialM: Int
        if (editingTimeIndex in notificationTimes.indices) {
            val parts = notificationTimes[editingTimeIndex].split(":")
            initialH = parts.getOrNull(0)?.toIntOrNull() ?: 9
            initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        } else {
            val nowCal = Calendar.getInstance()
            initialH = nowCal.get(Calendar.HOUR_OF_DAY)
            initialM = nowCal.get(Calendar.MINUTE)
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialH,
            initialMinute = initialM,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour
                    val m = timePickerState.minute
                    val formatted = String.format("%02d:%02d", h, m)
                    if (editingTimeIndex in notificationTimes.indices) {
                        notificationTimes = notificationTimes.toMutableList().apply {
                            set(editingTimeIndex, formatted)
                        }
                    } else {
                        if (!notificationTimes.contains(formatted)) {
                            notificationTimes = notificationTimes + formatted
                        }
                    }
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Frequency Dropdown
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyDropdown(
    frequency: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onFrequencySelected: (String) -> Unit
) {
    val options = listOf(
        "One-Time",
        "Specific Days",
        "Monthly"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) }
    ) {
        OutlinedTextField(
            value = frequency,
            onValueChange = {},
            readOnly = true,
            label = { Text("Repeat Frequency") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontWeight = if (option == frequency) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onFrequencySelected(option) }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Days Schedule Card (For Daily / Specific Days)
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaysScheduleCard(
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit,
    intervalWeeks: Int,
    onIntervalWeeksChange: (Int) -> Unit,
    primaryHour: Int,
    primaryMinute: Int,
    extraTimes: List<String>
) {
    val weekdays = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )
    val weekends = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
    val all7Days = weekdays + weekends

    val daysList = listOf(
        "M" to Calendar.MONDAY,
        "T" to Calendar.TUESDAY,
        "W" to Calendar.WEDNESDAY,
        "T" to Calendar.THURSDAY,
        "F" to Calendar.FRIDAY,
        "S" to Calendar.SATURDAY,
        "S" to Calendar.SUNDAY
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick preset chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDays.containsAll(all7Days),
                    onClick = { onDaysChange(all7Days) },
                    label = { Text("All Days") }
                )
                FilterChip(
                    selected = selectedDays == weekdays,
                    onClick = { onDaysChange(weekdays) },
                    label = { Text("Weekdays") }
                )
                FilterChip(
                    selected = selectedDays == weekends,
                    onClick = { onDaysChange(weekends) },
                    label = { Text("Weekends") }
                )
            }

            // Day circles M T W T F S S
            Text("Select Days:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysList.forEach { (label, value) ->
                    val isSelected = selectedDays.contains(value)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                val updated = if (isSelected) selectedDays - value else selectedDays + value
                                onDaysChange(updated)
                            }
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider()

            // Recurrence cycle dropdown
            var expandedInterval by remember { mutableStateOf(false) }
            var showCustomWeeksDialog by remember { mutableStateOf(false) }
            val intervalOptions = listOf(
                1 to "Every week",
                2 to "Every other week",
                -1 to "Custom..."
            )

            ExposedDropdownMenuBox(
                expanded = expandedInterval,
                onExpandedChange = { expandedInterval = !expandedInterval }
            ) {
                OutlinedTextField(
                    value = FrequencyFormatter.formatWeekCycle(intervalWeeks),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Recurrence Cycle") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterval) }
                )
                ExposedDropdownMenu(
                    expanded = expandedInterval,
                    onDismissRequest = { expandedInterval = false }
                ) {
                    intervalOptions.forEach { (weeks, text) ->
                        DropdownMenuItem(
                            text = { Text(text, fontWeight = if (weeks == intervalWeeks) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                expandedInterval = false
                                if (weeks == -1) {
                                    showCustomWeeksDialog = true
                                } else {
                                    onIntervalWeeksChange(weeks)
                                }
                            }
                        )
                    }
                }
            }

            if (showCustomWeeksDialog) {
                var tempWeeksInput by remember { mutableStateOf("$intervalWeeks") }
                AlertDialog(
                    onDismissRequest = { showCustomWeeksDialog = false },
                    title = { Text("Custom Week Interval") },
                    text = {
                        OutlinedTextField(
                            value = tempWeeksInput,
                            onValueChange = { if (it.all { c -> c.isDigit() }) tempWeeksInput = it },
                            label = { Text("Repeat every N weeks") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val n = tempWeeksInput.toIntOrNull()
                            if (n != null && n in 1..52) {
                                onIntervalWeeksChange(n)
                                showCustomWeeksDialog = false
                            }
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomWeeksDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // Detailed explanation banner
            SchedulePreviewBanner(
                text = FrequencyFormatter.getDaysScheduleExplanation(
                    selectedDays = selectedDays,
                    intervalWeeks = intervalWeeks,
                    primaryHour = primaryHour,
                    primaryMinute = primaryMinute,
                    extraTimes = extraTimes
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Monthly Schedule Card
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyScheduleCard(
    monthlyType: String,
    onMonthlyTypeChange: (String) -> Unit,
    monthlyDay: Int,
    onMonthlyDayChange: (Int) -> Unit,
    weekOrdinal: Int,
    onWeekOrdinalChange: (Int) -> Unit,
    dayOfWeek: Int,
    onDayOfWeekChange: (Int) -> Unit,
    intervalMonths: Int,
    onIntervalMonthsChange: (Int) -> Unit,
    primaryHour: Int,
    primaryMinute: Int,
    extraTimes: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Pattern mode selector: By Date vs By Weekday
            Text("Monthly Rule:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = monthlyType == "day_of_month",
                    onClick = { onMonthlyTypeChange("day_of_month") },
                    label = { Text("By Date of Month") }
                )
                FilterChip(
                    selected = monthlyType == "day_of_week",
                    onClick = { onMonthlyTypeChange("day_of_week") },
                    label = { Text("By Day of Week") }
                )
            }

            // Mode 1: By Date of Month (1st, 2nd, ... 31st, Last Day)
            if (monthlyType == "day_of_month") {
                var expandedDay by remember { mutableStateOf(false) }
                val dayOptions = buildList {
                    for (d in 1..31) {
                        val suffix = when (d) {
                            1, 21, 31 -> "${d}st"
                            2, 22 -> "${d}nd"
                            3, 23 -> "${d}rd"
                            else -> "${d}th"
                        }
                        add(d to suffix)
                    }
                    add(Reminder.LAST_DAY_OF_MONTH to "Last Day of the Month")
                }

                ExposedDropdownMenuBox(
                    expanded = expandedDay,
                    onExpandedChange = { expandedDay = !expandedDay }
                ) {
                    val currentText = dayOptions.firstOrNull { it.first == monthlyDay }?.second
                        ?: if (monthlyDay == Reminder.LAST_DAY_OF_MONTH) "Last Day of the Month" else "${monthlyDay}th"

                    OutlinedTextField(
                        value = currentText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Month") },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) }
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDay,
                        onDismissRequest = { expandedDay = false }
                    ) {
                        dayOptions.forEach { (d, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        label,
                                        fontWeight = if (d == Reminder.LAST_DAY_OF_MONTH) FontWeight.Bold else FontWeight.Normal,
                                        color = if (d == Reminder.LAST_DAY_OF_MONTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onMonthlyDayChange(d)
                                    expandedDay = false
                                }
                            )
                        }
                    }
                }
            } else {
                // Mode 2: By Day of Week (e.g. 2nd Tuesday)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ordinal
                    var expandedOrd by remember { mutableStateOf(false) }
                    val ordinals = listOf(
                        1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th",
                        Reminder.LAST_WEEK_ORDINAL to "Last"
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedOrd,
                        onExpandedChange = { expandedOrd = !expandedOrd },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = ordinals.firstOrNull { it.first == weekOrdinal }?.second ?: "1st",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Occurrence") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOrd) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedOrd,
                            onDismissRequest = { expandedOrd = false }
                        ) {
                            ordinals.forEach { (ordVal, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onWeekOrdinalChange(ordVal)
                                        expandedOrd = false
                                    }
                                )
                            }
                        }
                    }

                    // Weekday
                    var expandedWd by remember { mutableStateOf(false) }
                    val weekdaysList = listOf(
                        Calendar.MONDAY to "Monday",
                        Calendar.TUESDAY to "Tuesday",
                        Calendar.WEDNESDAY to "Wednesday",
                        Calendar.THURSDAY to "Thursday",
                        Calendar.FRIDAY to "Friday",
                        Calendar.SATURDAY to "Saturday",
                        Calendar.SUNDAY to "Sunday"
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedWd,
                        onExpandedChange = { expandedWd = !expandedWd },
                        modifier = Modifier.weight(1.5f)
                    ) {
                        OutlinedTextField(
                            value = weekdaysList.firstOrNull { it.first == dayOfWeek }?.second ?: "Tuesday",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Weekday") },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWd) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedWd,
                            onDismissRequest = { expandedWd = false }
                        ) {
                            weekdaysList.forEach { (dwVal, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onDayOfWeekChange(dwVal)
                                        expandedWd = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Repeat every N months
            var expandedMonthInterval by remember { mutableStateOf(false) }
            var showCustomMonthsDialog by remember { mutableStateOf(false) }
            val monthIntervalOptions = listOf(
                1 to "Every month",
                6 to "Every 6th month",
                12 to "Every year",
                -1 to "Custom..."
            )

            ExposedDropdownMenuBox(
                expanded = expandedMonthInterval,
                onExpandedChange = { expandedMonthInterval = !expandedMonthInterval }
            ) {
                OutlinedTextField(
                    value = FrequencyFormatter.formatMonthCycle(intervalMonths),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Recurrence Cycle") },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonthInterval) }
                )
                ExposedDropdownMenu(
                    expanded = expandedMonthInterval,
                    onDismissRequest = { expandedMonthInterval = false }
                ) {
                    monthIntervalOptions.forEach { (m, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontWeight = if (m == intervalMonths) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                expandedMonthInterval = false
                                if (m == -1) {
                                    showCustomMonthsDialog = true
                                } else {
                                    onIntervalMonthsChange(m)
                                }
                            }
                        )
                    }
                }
            }

            if (showCustomMonthsDialog) {
                var tempMonthsInput by remember { mutableStateOf("$intervalMonths") }
                AlertDialog(
                    onDismissRequest = { showCustomMonthsDialog = false },
                    title = { Text("Custom Month Interval") },
                    text = {
                        OutlinedTextField(
                            value = tempMonthsInput,
                            onValueChange = { if (it.all { c -> c.isDigit() }) tempMonthsInput = it },
                            label = { Text("Repeat every N months") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val n = tempMonthsInput.toIntOrNull()
                            if (n != null && n in 1..60) {
                                onIntervalMonthsChange(n)
                                showCustomMonthsDialog = false
                            }
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomMonthsDialog = false }) { Text("Cancel") }
                    }
                )
            }

            // Detailed explanation banner
            SchedulePreviewBanner(
                text = FrequencyFormatter.getMonthlyScheduleExplanation(
                    monthlyType = monthlyType,
                    monthlyDay = monthlyDay,
                    weekOrdinal = weekOrdinal,
                    dayOfWeek = dayOfWeek,
                    intervalMonths = intervalMonths,
                    primaryHour = primaryHour,
                    primaryMinute = primaryMinute,
                    extraTimes = extraTimes
                )
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Supporting Composables
// ════════════════════════════════════════════════════════════════════

@Composable
private fun DatePickerCard(
    dateMillis: Long,
    formatter: SimpleDateFormat,
    onClick: () -> Unit,
    primaryHour: Int,
    primaryMinute: Int,
    extraTimes: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatter.format(Date(dateMillis)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            SchedulePreviewBanner(
                text = FrequencyFormatter.getOneTimeExplanation(
                    dateMillis = dateMillis,
                    primaryHour = primaryHour,
                    primaryMinute = primaryMinute,
                    extraTimes = extraTimes
                )
            )
        }
    }
}

@Composable
private fun SchedulePreviewBanner(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Helpers & Save Logic
// ════════════════════════════════════════════════════════════════════

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%d:%02d %s", h, minute, amPm)
}

private fun formatTimeStr(timeStr: String): String {
    val parts = timeStr.split(":")
    if (parts.size != 2) return timeStr
    val h = parts[0].toIntOrNull() ?: return timeStr
    val m = parts[1].toIntOrNull() ?: return timeStr
    return formatTime(h, m)
}

private fun saveReminder(
    context: android.content.Context,
    existing: Reminder?,
    title: String,
    desc: String,
    frequency: String,
    customDays: Set<Int>,
    selectedDateMillis: Long,
    notificationTimes: List<String>,
    intervalDays: Int,
    intervalWeeks: Int,
    monthlyType: String,
    monthlyDay: Int,
    monthlyWeekOrdinal: Int,
    monthlyDayOfWeek: Int,
    intervalMonths: Int,
    onBack: () -> Unit
) {
    if (title.isBlank()) {
        Toast.makeText(context, "Title required", Toast.LENGTH_SHORT).show()
        return
    }

    if (notificationTimes.isEmpty()) {
        Toast.makeText(context, "Please add at least one notification time", Toast.LENGTH_SHORT).show()
        return
    }

    val primaryParts = notificationTimes.first().split(":")
    val selectedHour = primaryParts.getOrNull(0)?.toIntOrNull() ?: 0
    val selectedMinute = primaryParts.getOrNull(1)?.toIntOrNull() ?: 0
    val extraTimes = if (notificationTimes.size > 1) notificationTimes.drop(1) else emptyList()

    val triggerCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, selectedHour)
        set(Calendar.MINUTE, selectedMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val actualFrequency = when (frequency) {
        "Daily" -> if (customDays.size == 7) "Daily" else "Specific Days"
        else -> frequency
    }

    when (actualFrequency) {
        "One-Time" -> {
            triggerCal.timeInMillis = selectedDateMillis
            triggerCal.set(Calendar.HOUR_OF_DAY, selectedHour)
            triggerCal.set(Calendar.MINUTE, selectedMinute)
            triggerCal.set(Calendar.SECOND, 0)
            triggerCal.set(Calendar.MILLISECOND, 0)
        }
        "Daily", "Specific Days", "Weekly" -> {
            if (customDays.isEmpty()) {
                Toast.makeText(context, "Select at least one day", Toast.LENGTH_SHORT).show()
                return
            }
            triggerCal.timeInMillis = AlarmScheduler.getNextWeeklyMillis(
                selectedHour, selectedMinute, customDays, intervalWeeks
            )
        }
        "Monthly" -> {
            val tempReminder = Reminder(
                id = 0, title = "", desc = "", timeInMillis = 0,
                frequency = "Monthly",
                monthlyType = monthlyType,
                monthlyDay = monthlyDay,
                monthlyWeekOrdinal = monthlyWeekOrdinal,
                monthlyDayOfWeek = monthlyDayOfWeek,
                intervalMonths = intervalMonths
            )
            triggerCal.timeInMillis = AlarmScheduler.getNextMonthlyMillis(
                selectedHour, selectedMinute, tempReminder
            )
        }
    }

    if (triggerCal.timeInMillis <= System.currentTimeMillis() && actualFrequency == "One-Time") {
        Toast.makeText(context, "Time must be in the future", Toast.LENGTH_SHORT).show()
        return
    }

    val reminder = Reminder(
        id = existing?.id ?: System.currentTimeMillis().toInt(),
        title = title,
        desc = desc,
        timeInMillis = triggerCal.timeInMillis,
        frequency = actualFrequency,
        customDays = customDays.toList(),
        extraTimes = extraTimes,
        intervalDays = intervalDays,
        intervalWeeks = intervalWeeks,
        monthlyType = monthlyType,
        monthlyDay = monthlyDay,
        monthlyWeekOrdinal = monthlyWeekOrdinal,
        monthlyDayOfWeek = monthlyDayOfWeek,
        intervalMonths = intervalMonths
    )

    if (existing != null) {
        AlarmScheduler.cancel(context, existing.id)
    }

    ReminderStorage.saveReminder(context, reminder)
    AlarmScheduler.schedule(context, reminder)
    AppState.forceRefresh += 1
    onBack()
}
