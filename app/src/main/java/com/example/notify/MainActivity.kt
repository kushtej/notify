package com.example.notify

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    override fun onResume() {
        super.onResume()
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