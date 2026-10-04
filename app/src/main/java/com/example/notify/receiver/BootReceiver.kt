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
            // Only re-schedule reminders that are still active and in the future
            if (!reminder.isCompleted && reminder.timeInMillis > now) {
                AlarmScheduler.schedule(context, reminder)
            }
        }
    }
}
