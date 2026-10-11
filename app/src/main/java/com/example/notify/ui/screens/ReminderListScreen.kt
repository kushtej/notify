package com.example.notify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notify.data.ReminderStorage
import com.example.notify.model.Reminder
import com.example.notify.ui.AppState
import com.example.notify.ui.FrequencyFormatter
import com.example.notify.ui.richtext.RichTextFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    title: String,
    list: List<Reminder>,
    emptyText: String,
    showAdd: Boolean,
    showEdit: Boolean,
    showDelete: Boolean,
    showClearAll: Boolean,
    onOpenDrawer: () -> Unit,
    onAdd: () -> Unit = {},
    onView: (Int) -> Unit = {},
    onEdit: (Int) -> Unit = {},
    onDelete: (Int) -> Unit = {},
    onClearAll: () -> Unit = {}
) {
    val context = LocalContext.current
    var showClearConfirm by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("all") }

    // Section collapse states
    var expandedToday by remember { mutableStateOf(true) }
    var expandedUpcoming by remember { mutableStateOf(true) }
    var expandedRepeating by remember { mutableStateOf(true) }
    var expandedYesterday by remember { mutableStateOf(true) }
    var expandedEarlier by remember { mutableStateOf(true) }

    // User-configured filter chips
    val filterConfigs = remember(list, AppState.forceRefresh) {
        ReminderStorage.getFilterConfigs(context)
    }
    val activeFilterConfigs = remember(filterConfigs) {
        filterConfigs.filter { it.enabled }
    }

    // Reset selected filter to "all" if current choice is no longer enabled
    LaunchedEffect(activeFilterConfigs) {
        if (selectedFilter != "all" && activeFilterConfigs.none { it.id == selectedFilter }) {
            selectedFilter = "all"
        }
    }

    // Time calculations
    val nowMillis = System.currentTimeMillis()
    val nowCal = remember(list, nowMillis) { Calendar.getInstance().apply { timeInMillis = nowMillis } }

    val todayItems = remember(list, nowMillis) { list.filter { triggersOnDate(it, nowCal) } }
    val upcoming1hItems = remember(list, nowMillis) { list.filter { triggersWithinOneHour(it, nowMillis) } }
    val allRecurringItems = remember(list) { list.filter { it.frequency != "One-Time" } }
    val weeklyItems = remember(list) {
        list.filter { it.frequency in listOf("Weekly", "Specific Days", "Daily / Weekly", "Days of the Week") || (it.frequency == "Daily" && it.customDays.isNotEmpty()) }
    }
    val monthlyItems = remember(list) { list.filter { it.frequency == "Monthly" } }
    val upcomingOneTimeItems = remember(list, nowMillis) { list.filter { isUpcomingOneTime(it, nowCal) } }
    val otherRecurringItems = remember(list, nowMillis) { allRecurringItems.filter { !triggersOnDate(it, nowCal) } }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notifications...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (list.isNotEmpty()) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        if (showClearAll && list.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirm = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Clear All",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (showAdd) {
                FloatingActionButton(
                    onClick = onAdd,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        if (list.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Quick Filter Chips: Only show if Active screen, not searching, AND user has at least one filter enabled
                if (title == "Active Reminders" && !isSearchActive && activeFilterConfigs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "all",
                                onClick = { selectedFilter = "all" },
                                label = { Text("All (${list.size})") }
                            )
                        }
                        items(activeFilterConfigs, key = { it.id }) { cfg ->
                            val count = when (cfg.id) {
                                "today" -> todayItems.size
                                "upcoming" -> upcoming1hItems.size
                                "repeating" -> allRecurringItems.size
                                "weekly" -> weeklyItems.size
                                "monthly" -> monthlyItems.size
                                else -> 0
                            }
                            FilterChip(
                                selected = selectedFilter == cfg.id,
                                onClick = { selectedFilter = cfg.id },
                                label = { Text("${cfg.label} ($count)") }
                            )
                        }
                    }
                }

                // Reminder List Content
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Search mode: show flat matching results
                    if (searchQuery.isNotBlank()) {
                        val matching = list.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                it.desc.contains(searchQuery, ignoreCase = true)
                        }
                        if (matching.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No notifications matching \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(matching, key = { it.id }) { reminder ->
                                ReminderCard(
                                    reminder = reminder,
                                    showEdit = showEdit,
                                    showDelete = showDelete,
                                    onView = onView,
                                    onEdit = onEdit,
                                    onDelete = onDelete
                                )
                            }
                        }
                    } else if (title == "Active Reminders") {
                        when (selectedFilter) {
                            "today" -> {
                                if (todayItems.isEmpty()) {
                                    item { EmptyStateMessage("No notifications scheduled for today.") }
                                } else {
                                    items(todayItems, key = { it.id }) { reminder ->
                                        ReminderCard(
                                            reminder = reminder,
                                            showEdit = showEdit,
                                            showDelete = showDelete,
                                            onView = onView,
                                            onEdit = onEdit,
                                            onDelete = onDelete
                                        )
                                    }
                                }
                            }

                            "upcoming" -> {
                                if (upcoming1hItems.isEmpty()) {
                                    item { EmptyStateMessage("No notifications scheduled within the next 1 hour.") }
                                } else {
                                    items(upcoming1hItems, key = { it.id }) { reminder ->
                                        ReminderCard(
                                            reminder = reminder,
                                            showEdit = showEdit,
                                            showDelete = showDelete,
                                            onView = onView,
                                            onEdit = onEdit,
                                            onDelete = onDelete
                                        )
                                    }
                                }
                            }

                            "repeating" -> {
                                if (allRecurringItems.isEmpty()) {
                                    item { EmptyStateMessage("No repeating notifications configured.") }
                                } else {
                                    items(allRecurringItems, key = { it.id }) { reminder ->
                                        ReminderCard(
                                            reminder = reminder,
                                            showEdit = showEdit,
                                            showDelete = showDelete,
                                            onView = onView,
                                            onEdit = onEdit,
                                            onDelete = onDelete
                                        )
                                    }
                                }
                            }

                            "weekly" -> {
                                if (weeklyItems.isEmpty()) {
                                    item { EmptyStateMessage("No weekly notifications configured.") }
                                } else {
                                    items(weeklyItems, key = { it.id }) { reminder ->
                                        ReminderCard(
                                            reminder = reminder,
                                            showEdit = showEdit,
                                            showDelete = showDelete,
                                            onView = onView,
                                            onEdit = onEdit,
                                            onDelete = onDelete
                                        )
                                    }
                                }
                            }

                            "monthly" -> {
                                if (monthlyItems.isEmpty()) {
                                    item { EmptyStateMessage("No monthly notifications configured.") }
                                } else {
                                    items(monthlyItems, key = { it.id }) { reminder ->
                                        ReminderCard(
                                            reminder = reminder,
                                            showEdit = showEdit,
                                            showDelete = showDelete,
                                            onView = onView,
                                            onEdit = onEdit,
                                            onDelete = onDelete
                                        )
                                    }
                                }
                            }

                            else -> {
                                // "all": Grouped into collapsible sections without duplication
                                if (todayItems.isNotEmpty()) {
                                    item {
                                        SectionHeader(
                                            title = "Due Today",
                                            count = todayItems.size,
                                            isExpanded = expandedToday,
                                            onToggle = { expandedToday = !expandedToday }
                                        )
                                    }
                                    if (expandedToday) {
                                        items(todayItems, key = { it.id }) { reminder ->
                                            ReminderCard(
                                                reminder = reminder,
                                                showEdit = showEdit,
                                                showDelete = showDelete,
                                                onView = onView,
                                                onEdit = onEdit,
                                                onDelete = onDelete
                                            )
                                        }
                                    }
                                }

                                if (upcomingOneTimeItems.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SectionHeader(
                                            title = "Upcoming One-Time",
                                            count = upcomingOneTimeItems.size,
                                            isExpanded = expandedUpcoming,
                                            onToggle = { expandedUpcoming = !expandedUpcoming }
                                        )
                                    }
                                    if (expandedUpcoming) {
                                        items(upcomingOneTimeItems, key = { it.id }) { reminder ->
                                            ReminderCard(
                                                reminder = reminder,
                                                showEdit = showEdit,
                                                showDelete = showDelete,
                                                onView = onView,
                                                onEdit = onEdit,
                                                onDelete = onDelete
                                            )
                                        }
                                    }
                                }

                                if (otherRecurringItems.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        SectionHeader(
                                            title = "Repeating Schedules",
                                            count = otherRecurringItems.size,
                                            isExpanded = expandedRepeating,
                                            onToggle = { expandedRepeating = !expandedRepeating }
                                        )
                                    }
                                    if (expandedRepeating) {
                                        items(otherRecurringItems, key = { it.id }) { reminder ->
                                            ReminderCard(
                                                reminder = reminder,
                                                showEdit = showEdit,
                                                showDelete = showDelete,
                                                onView = onView,
                                                onEdit = onEdit,
                                                onDelete = onDelete
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (title == "History") {
                        // History screen: group by Today, Yesterday, Earlier
                        val groups = groupHistory(list)
                        val todayHist = groups["Today"] ?: emptyList()
                        val yestHist = groups["Yesterday"] ?: emptyList()
                        val earlierHist = groups["Earlier"] ?: emptyList()

                        if (todayHist.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Today",
                                    count = todayHist.size,
                                    isExpanded = expandedToday,
                                    onToggle = { expandedToday = !expandedToday }
                                )
                            }
                            if (expandedToday) {
                                items(todayHist, key = { it.id }) { reminder ->
                                    ReminderCard(
                                        reminder = reminder,
                                        showEdit = showEdit,
                                        showDelete = showDelete,
                                        onView = onView,
                                        onEdit = onEdit,
                                        onDelete = onDelete
                                    )
                                }
                            }
                        }

                        if (yestHist.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                SectionHeader(
                                    title = "Yesterday",
                                    count = yestHist.size,
                                    isExpanded = expandedYesterday,
                                    onToggle = { expandedYesterday = !expandedYesterday }
                                )
                            }
                            if (expandedYesterday) {
                                items(yestHist, key = { it.id }) { reminder ->
                                    ReminderCard(
                                        reminder = reminder,
                                        showEdit = showEdit,
                                        showDelete = showDelete,
                                        onView = onView,
                                        onEdit = onEdit,
                                        onDelete = onDelete
                                    )
                                }
                            }
                        }

                        if (earlierHist.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                SectionHeader(
                                    title = "Earlier",
                                    count = earlierHist.size,
                                    isExpanded = expandedEarlier,
                                    onToggle = { expandedEarlier = !expandedEarlier }
                                )
                            }
                            if (expandedEarlier) {
                                items(earlierHist, key = { it.id }) { reminder ->
                                    ReminderCard(
                                        reminder = reminder,
                                        showEdit = showEdit,
                                        showDelete = showDelete,
                                        onView = onView,
                                        onEdit = onEdit,
                                        onDelete = onDelete
                                    )
                                }
                            }
                        }
                    } else {
                        // Other screens (e.g. Snoozed)
                        items(list, key = { it.id }) { reminder ->
                            ReminderCard(
                                reminder = reminder,
                                showEdit = showEdit,
                                showDelete = showDelete,
                                onView = onView,
                                onEdit = onEdit,
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ClearAllConfirmDialog(
            onConfirm = { onClearAll(); showClearConfirm = false },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    showEdit: Boolean,
    showDelete: Boolean,
    onView: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("EEE, MMM dd \u2022 hh:mm a", Locale.getDefault())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView(reminder.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (reminder.desc.isNotEmpty()) {
                    Text(
                        text = RichTextFormatter.toPlainTextSummary(reminder.desc),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val timeSummary = if (reminder.extraTimes.isNotEmpty()) {
                    "${dateFormatter.format(Date(reminder.timeInMillis))} (+${reminder.extraTimes.size} more)"
                } else {
                    dateFormatter.format(Date(reminder.timeInMillis))
                }
                Text(
                    text = timeSummary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                if (reminder.frequency != "One-Time") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\u21BB ${FrequencyFormatter.formatFrequency(reminder)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (showEdit) {
                IconButton(onClick = { onEdit(reminder.id) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
            }
            if (showDelete) {
                IconButton(onClick = { onDelete(reminder.id) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClearAllConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear All") },
        text = { Text("Are you sure you want to delete these notifications?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear All", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Time categorization helpers ──────────────────────────────────────

internal fun triggersOnDate(reminder: Reminder, cal: Calendar): Boolean {
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

    return when (reminder.frequency) {
        "One-Time" -> {
            val rCal = Calendar.getInstance().apply { timeInMillis = reminder.timeInMillis }
            rCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                rCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
        }
        "Daily" -> true
        "Specific Days", "Weekly", "Daily / Weekly", "Days of the Week" -> {
            if (reminder.customDays.isEmpty()) true
            else if (!reminder.customDays.contains(dayOfWeek)) false
            else {
                if (reminder.intervalWeeks > 1 && reminder.startMillis > 0L) {
                    val startW = com.example.notify.scheduler.AlarmScheduler.getStartOfWeek(reminder.startMillis)
                    val calW = com.example.notify.scheduler.AlarmScheduler.getStartOfWeek(cal.timeInMillis)
                    val diffWeeks = ((calW.timeInMillis - startW.timeInMillis) / (7L * 24 * 3600 * 1000)).toInt()
                    diffWeeks >= 0 && (diffWeeks % reminder.intervalWeeks == 0)
                } else {
                    true
                }
            }
        }
        "Monthly" -> {
            val inMonthCycle = if (reminder.intervalMonths > 1 && reminder.startMillis > 0L) {
                val startCal = Calendar.getInstance().apply { timeInMillis = reminder.startMillis }
                val diffMonths = (cal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 + (cal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH))
                diffMonths >= 0 && (diffMonths % reminder.intervalMonths == 0)
            } else {
                true
            }
            if (!inMonthCycle) false
            else if (reminder.monthlyType == "day_of_month") {
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (reminder.monthlyDay == Reminder.LAST_DAY_OF_MONTH) {
                    dayOfMonth == lastDay
                } else {
                    dayOfMonth == reminder.monthlyDay
                }
            } else {
                if (dayOfWeek != reminder.monthlyDayOfWeek) false
                else {
                    val currentOrdinal = (dayOfMonth - 1) / 7 + 1
                    val isLast = (dayOfMonth + 7) > cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    if (reminder.monthlyWeekOrdinal == Reminder.LAST_WEEK_ORDINAL) isLast
                    else currentOrdinal == reminder.monthlyWeekOrdinal
                }
            }
        }
        else -> false
    }
}

internal fun triggersWithinOneHour(reminder: Reminder, nowMillis: Long = System.currentTimeMillis()): Boolean {
    val windowEnd = nowMillis + 3600_000L

    if (reminder.frequency == "One-Time") {
        return reminder.timeInMillis in (nowMillis + 1)..windowEnd
    }

    val allTimes = mutableListOf<Pair<Int, Int>>()
    val primaryCal = Calendar.getInstance().apply { timeInMillis = reminder.timeInMillis }
    allTimes.add(primaryCal.get(Calendar.HOUR_OF_DAY) to primaryCal.get(Calendar.MINUTE))

    reminder.extraTimes.forEach { extra ->
        val parts = extra.split(":")
        if (parts.size == 2) {
            val h = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            if (h != null && m != null) {
                allTimes.add(h to m)
            }
        }
    }

    val todayCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val tomorrowCal = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

    for (dayCal in listOf(todayCal, tomorrowCal)) {
        if (triggersOnDate(reminder, dayCal)) {
            for ((h, m) in allTimes) {
                val triggerCal = (dayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (triggerCal.timeInMillis in (nowMillis + 1)..windowEnd) {
                    return true
                }
            }
        }
    }

    return false
}

internal fun isUpcomingOneTime(reminder: Reminder, nowCal: Calendar): Boolean {
    if (reminder.frequency != "One-Time") return false
    val rCal = Calendar.getInstance().apply { timeInMillis = reminder.timeInMillis }
    val todayYear = nowCal.get(Calendar.YEAR)
    val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)
    return rCal.get(Calendar.YEAR) > todayYear ||
        (rCal.get(Calendar.YEAR) == todayYear && rCal.get(Calendar.DAY_OF_YEAR) > todayDayOfYear)
}

internal fun groupHistory(list: List<Reminder>): Map<String, List<Reminder>> {
    val nowCal = Calendar.getInstance()
    val todayYear = nowCal.get(Calendar.YEAR)
    val todayDay = nowCal.get(Calendar.DAY_OF_YEAR)

    val todayList = mutableListOf<Reminder>()
    val yesterdayList = mutableListOf<Reminder>()
    val earlierList = mutableListOf<Reminder>()

    list.forEach { r ->
        val c = Calendar.getInstance().apply { timeInMillis = r.timeInMillis }
        val y = c.get(Calendar.YEAR)
        val d = c.get(Calendar.DAY_OF_YEAR)
        if (y == todayYear && d == todayDay) {
            todayList.add(r)
        } else if (y == todayYear && d == todayDay - 1) {
            yesterdayList.add(r)
        } else {
            earlierList.add(r)
        }
    }

    return buildMap {
        if (todayList.isNotEmpty()) put("Today", todayList)
        if (yesterdayList.isNotEmpty()) put("Yesterday", yesterdayList)
        if (earlierList.isNotEmpty()) put("Earlier", earlierList)
    }
}
