package com.example.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.notify.data.ReminderStorage
import com.example.notify.scheduler.AlarmScheduler
import com.example.notify.ui.AppState
import com.example.notify.ui.NotifyApp

/**
 * Entry point for the Notify app.
 * Handles permissions, theming, notification intents, and hosts the root composable.
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationChannel()
        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)

        setContent {
            val context = LocalContext.current
            val themeTrigger = AppState.themeRefresh
            val themePref = remember(themeTrigger) { ReminderStorage.getTheme(context) }

            val isDark = when (themePref) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            val colors = if (isDark) {
                darkColorScheme(
                    primary = Color(0xFF66B2FF),
                    surfaceVariant = Color(0xFF2D2D2D)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF005C99),
                    surfaceVariant = Color(0xFFF0F4F8)
                )
            }

            MaterialTheme(colorScheme = colors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NotifyApp()
                }
            }
        }
    }

    private var hasPromptedExactAlarm = false

    override fun onResume() {
        super.onResume()
        ensureExactAlarmPermission()
        AlarmScheduler.rescheduleAll(this)
        ReminderStorage.pruneOldCompleted(this)
        AppState.forceRefresh += 1
    }

    /**
     * Called when the activity is re-launched while already running (singleTask launch mode).
     * Handles notification taps when the app is already in the foreground.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * Creates the notification channel eagerly on startup so it's always
     * available before the first alarm fires.
     */
    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "notify_channel",
                "Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm reminders"
                enableVibration(true)
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * On Android 12+ (API 31), exact alarms require user permission.
     * If not granted, open the system settings page to let the user enable it.
     */
    private fun ensureExactAlarmPermission() {
        if (hasPromptedExactAlarm) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                hasPromptedExactAlarm = true
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Opens the detail view for a reminder when launched from a notification tap.
     */
    private fun handleNotificationIntent(intent: Intent?) {
        val viewId = intent?.getIntExtra("EDIT_ID", -1) ?: -1
        if (viewId != -1) {
            AppState.viewingId = viewId
        }
    }
}