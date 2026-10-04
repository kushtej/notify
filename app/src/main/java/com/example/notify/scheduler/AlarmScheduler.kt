package com.example.notify.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.notify.model.Reminder
import com.example.notify.receiver.AlarmReceiver
import java.util.Calendar

/**
 * Handles scheduling and cancelling alarm-based reminders.
 */
object AlarmScheduler {

    /**
     * Schedules an exact alarm for the given reminder.
     */
    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminder.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timeInMillis, pendingIntent)
    }

    /**
     * Cancels a previously scheduled alarm for the given reminder ID.
     */
    fun cancel(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Calculates the next trigger time for a reminder on specific days of the week.
     */
    fun getNextDayMillis(hour: Int, minute: Int, allowedDays: Set<Int>): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0..7) {
            if (i > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
            if (allowedDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                if (cal.timeInMillis > System.currentTimeMillis()) return cal.timeInMillis
            }
        }
        return cal.timeInMillis
    }
}
