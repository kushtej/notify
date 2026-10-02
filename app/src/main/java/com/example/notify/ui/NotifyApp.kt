package com.example.notify.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notify.data.ReminderStorage
import com.example.notify.model.Screen
import com.example.notify.scheduler.AlarmScheduler
import com.example.notify.ui.screens.DetailScreen
import com.example.notify.ui.screens.EditorScreen
import com.example.notify.ui.screens.ReminderListScreen
import com.example.notify.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Root composable that manages navigation and the drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifyApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.Active) }
    var previousScreen by remember { mutableStateOf(Screen.Active) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val refresh = AppState.forceRefresh
    val historyEnabled = remember(refresh) { ReminderStorage.getAutoClearHours(context) > 0 }

    // Navigate to Detail when viewingId is set externally (e.g. from notification tap)
    LaunchedEffect(AppState.viewingId) {
        if (AppState.viewingId != null && currentScreen != Screen.Detail) {
            previousScreen = currentScreen
            currentScreen = Screen.Detail
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                currentScreen = currentScreen,
                historyEnabled = historyEnabled,
                onNavigate = { screen ->
                    currentScreen = screen
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        val allReminders = remember(refresh) { ReminderStorage.getAllReminders(context) }
        val active = allReminders.filter { !it.isCompleted && !it.isSnoozed }
        val snoozed = allReminders.filter { it.isSnoozed }
        val completed = allReminders.filter { it.isCompleted }.sortedByDescending { it.timeInMillis }

        when (currentScreen) {
            Screen.Active -> ReminderListScreen(
                title = "Active Reminders",
                list = active,
                emptyText = "No active reminders.",
                showAdd = true,
                showEdit = true,
                showDelete = true,
                showClearAll = false,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onAdd = { AppState.editingId = null; currentScreen = Screen.Edit },
                onView = { id ->
                    AppState.viewingId = id
                    previousScreen = Screen.Active
                    currentScreen = Screen.Detail
                },
                onEdit = { AppState.editingId = it; currentScreen = Screen.Edit },
                onDelete = {
                    ReminderStorage.deleteReminder(context, it)
                    AlarmScheduler.cancel(context, it)
                    AppState.forceRefresh += 1
                }
            )

            Screen.Snoozed -> ReminderListScreen(
                title = "Snoozed",
                list = snoozed,
                emptyText = "No snoozed notifications.",
                showAdd = false,
                showEdit = true,
                showDelete = true,
                showClearAll = true,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onView = { id ->
                    AppState.viewingId = id
                    previousScreen = Screen.Snoozed
                    currentScreen = Screen.Detail
                },
                onEdit = { AppState.editingId = it; currentScreen = Screen.Edit },
                onDelete = {
                    ReminderStorage.deleteReminder(context, it)
                    AlarmScheduler.cancel(context, it)
                    AppState.forceRefresh += 1
                },
                onClearAll = {
                    snoozed.forEach { AlarmScheduler.cancel(context, it.id) }
                    ReminderStorage.clearAllSnoozed(context)
                    AppState.forceRefresh += 1
                }
            )

            Screen.Completed -> ReminderListScreen(
                title = "History",
                list = completed,
                emptyText = "History is clear.",
                showAdd = false,
                showEdit = false,
                showDelete = false,
                showClearAll = true,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onView = { id ->
                    AppState.viewingId = id
                    previousScreen = Screen.Completed
                    currentScreen = Screen.Detail
                },
                onClearAll = {
                    ReminderStorage.clearAllCompleted(context)
                    AppState.forceRefresh += 1
                }
            )

            Screen.Detail -> DetailScreen(
                onBack = {
                    AppState.viewingId = null
                    currentScreen = previousScreen
                },
                onEdit = {
                    AppState.editingId = AppState.viewingId
                    AppState.viewingId = null
                    currentScreen = Screen.Edit
                },
                onDelete = {
                    val id = AppState.viewingId
                    if (id != null) {
                        ReminderStorage.deleteReminder(context, id)
                        AlarmScheduler.cancel(context, id)
                        AppState.forceRefresh += 1
                    }
                    AppState.viewingId = null
                    currentScreen = previousScreen
                }
            )

            Screen.Edit -> EditorScreen(
                onBack = { AppState.editingId = null; currentScreen = Screen.Active }
            )

            Screen.Settings -> SettingsScreen(
                onOpenDrawer = { scope.launch { drawerState.open() } }
            )
        }
    }
}

@Composable
private fun NavigationDrawerContent(
    currentScreen: Screen,
    historyEnabled: Boolean,
    onNavigate: (Screen) -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Notify",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Notifications, null) },
            label = { Text("Active") },
            selected = currentScreen == Screen.Active,
            onClick = { onNavigate(Screen.Active) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Refresh, null) },
            label = { Text("Snoozed") },
            selected = currentScreen == Screen.Snoozed,
            onClick = { onNavigate(Screen.Snoozed) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        if (historyEnabled) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.CheckCircle, null) },
                label = { Text("Completed") },
                selected = currentScreen == Screen.Completed,
                onClick = { onNavigate(Screen.Completed) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Settings") },
            selected = currentScreen == Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
