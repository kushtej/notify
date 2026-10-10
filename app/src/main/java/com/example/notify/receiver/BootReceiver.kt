package com.example.notify.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.notify.data.ReminderStorage
import com.example.notify.scheduler.AlarmScheduler

/**
 * Re-schedules all active (non-completed) reminders after the device reboots.
 *
 * Android cancels every alarm on reboot, so this receiver listens for
 * BOOT_COMPLETED and re-registers each pending reminder with the AlarmManager.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val reminders = ReminderStorage.getAllReminders(context)
        val now = System.currentTimeMillis()

        for (reminder in reminders) {
            if (reminder.isCompleted) continue

            if (reminder.timeInMillis > now) {
                // Still in the future, re-schedule
                AlarmScheduler.schedule(context, reminder)
            } else if (reminder.frequency != "One-Time") {
                // Recurring alarm that passed while device was turned off; advance to next future occurrence
                val nextTime = AlarmScheduler.calculateNextOccurrence(reminder)
                val updatedReminder = reminder.copy(timeInMillis = nextTime)
                ReminderStorage.saveReminder(context, updatedReminder)
                AlarmScheduler.schedule(context, updatedReminder)
            }
        }
    }
}
