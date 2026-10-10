package com.example.notify.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.notify.MainActivity
import com.example.notify.data.ReminderStorage
import com.example.notify.model.Reminder
import com.example.notify.scheduler.AlarmScheduler
import com.example.notify.ui.AppState
import java.util.Calendar

/**
 * Receives alarm broadcasts and handles firing notifications and snoozing.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE = "ACTION_FIRE"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val EXTRA_ID = "ID"
        const val EXTRA_TIME_INDEX = "TIME_INDEX"
        const val EXTRA_TITLE = "TITLE"
        const val EXTRA_DESC = "DESC"
        private const val CHANNEL_ID = "notify_channel"
        private const val CHANNEL_NAME = "Notifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SNOOZE -> handleSnooze(context, intent)
            ACTION_FIRE -> handleFire(context, intent)
        }
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(id)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val desc = intent.getStringExtra(EXTRA_DESC) ?: ""
        val snoozeMins = ReminderStorage.getSnoozeMins(context)
        val snoozeMillis = System.currentTimeMillis() + (snoozeMins * 60 * 1000)

        val snoozedReminder = Reminder(
            id = System.currentTimeMillis().toInt(),
            title = title,
            desc = desc,
            timeInMillis = snoozeMillis,
            frequency = "One-Time",
            customDays = emptyList(),
            isSnoozed = true
        )

        ReminderStorage.saveReminder(context, snoozedReminder)
        AlarmScheduler.schedule(context, snoozedReminder)
        AppState.forceRefresh += 1
    }

    private fun handleFire(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val reminder = ReminderStorage.getAllReminders(context).find { it.id == id } ?: return

        ensureNotificationChannel(manager)
        showNotification(context, manager, reminder)
        updateReminderState(context, reminder)
        AppState.forceRefresh += 1
    }

    private fun ensureNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, manager: NotificationManager, reminder: Reminder) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EDIT_ID", reminder.id)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, reminder.id, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_ID, reminder.id)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_DESC, reminder.desc)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, reminder.id, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(reminder.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, "Snooze", snoozePendingIntent)

        if (reminder.desc.isNotBlank()) {
            val summary = com.example.notify.ui.richtext.NotificationFormatter.toPlainTextSummary(reminder.desc)
            val spanned = com.example.notify.ui.richtext.NotificationFormatter.toNotificationSpanned(reminder.desc)
            builder.setContentText(summary)
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(reminder.title)
                    .bigText(spanned)
            )
        }

        val notification = builder.build()
        manager.notify(reminder.id, notification)
    }

    private fun updateReminderState(context: Context, reminder: Reminder) {
        val keepHistory = ReminderStorage.getAutoClearHours(context) > 0

        if (reminder.isSnoozed) {
            ReminderStorage.deleteReminder(context, reminder.id)
            if (keepHistory) {
                ReminderStorage.saveReminder(
                    context,
                    reminder.copy(id = System.currentTimeMillis().toInt(), isCompleted = true, isSnoozed = false)
                )
            }
        } else if (reminder.frequency == "One-Time") {
            if (keepHistory) {
                ReminderStorage.saveReminder(context, reminder.copy(isCompleted = true))
            } else {
                ReminderStorage.deleteReminder(context, reminder.id)
            }
        } else {
            if (keepHistory) {
                ReminderStorage.saveReminder(
                    context,
                    reminder.copy(id = System.currentTimeMillis().toInt(), isCompleted = true)
                )
            }
            scheduleNextRecurrence(context, reminder)
        }
    }

    private fun scheduleNextRecurrence(context: Context, reminder: Reminder) {
        val nextTime = AlarmScheduler.calculateNextOccurrence(reminder)
        val updatedReminder = reminder.copy(timeInMillis = nextTime)
        ReminderStorage.saveReminder(context, updatedReminder)
        AlarmScheduler.schedule(context, updatedReminder)
    }
}
