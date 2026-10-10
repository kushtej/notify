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
 *
 * For reminders with multiple times per day, separate alarms are scheduled
 * using sub-IDs: `reminder.id * 100 + timeIndex` (index 0 = primary time,
 * 1..N = extra times).
 */
object AlarmScheduler {

    /**
     * Schedules all alarms for the given reminder (primary + extra times).
     */
    fun schedule(context: Context, reminder: Reminder) {
        // Schedule primary alarm
        scheduleAlarm(context, reminder.id * 100, reminder.timeInMillis)

        // Schedule extra-time alarms
        val primaryCal = Calendar.getInstance().apply { timeInMillis = reminder.timeInMillis }
        reminder.extraTimes.forEachIndexed { index, timeStr ->
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: return@forEachIndexed
                val m = parts[1].toIntOrNull() ?: return@forEachIndexed
                val extraCal = Calendar.getInstance().apply {
                    timeInMillis = reminder.timeInMillis
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                // If this extra time is before now, advance it the same way as the primary
                if (extraCal.timeInMillis <= System.currentTimeMillis()) {
                    when (reminder.frequency) {
                        "Daily", "Specific Days", "Weekly" -> {
                            if (reminder.customDays.isNotEmpty()) {
                                val next = getNextWeeklyMillis(h, m, reminder.customDays.toSet(), reminder.intervalWeeks)
                                extraCal.timeInMillis = next
                            } else {
                                extraCal.add(Calendar.DAY_OF_YEAR, if (reminder.intervalDays > 1) reminder.intervalDays else 1)
                            }
                        }
                        "Monthly" -> {
                            val next = getNextMonthlyMillis(h, m, reminder)
                            extraCal.timeInMillis = next
                            extraCal.set(Calendar.HOUR_OF_DAY, h)
                            extraCal.set(Calendar.MINUTE, m)
                        }
                        else -> {
                            // One-Time: only schedule if in the future
                            if (extraCal.timeInMillis <= System.currentTimeMillis()) return@forEachIndexed
                        }
                    }
                }
                scheduleAlarm(context, reminder.id * 100 + index + 1, extraCal.timeInMillis)
            }
        }
    }

    /**
     * Cancels all alarms for a reminder (primary + up to 10 extra times).
     */
    fun cancel(context: Context, id: Int) {
        for (i in 0..10) {
            cancelAlarm(context, id * 100 + i)
        }
    }

    /**
     * Calculates the next future occurrence for a recurring reminder.
     */
    fun calculateNextOccurrence(reminder: Reminder): Long {
        val currentCal = Calendar.getInstance().apply {
            timeInMillis = reminder.timeInMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val hour = currentCal.get(Calendar.HOUR_OF_DAY)
        val minute = currentCal.get(Calendar.MINUTE)

        return when (reminder.frequency) {
            "Daily", "Specific Days", "Weekly" -> {
                if (reminder.customDays.isNotEmpty()) {
                    getNextWeeklyMillis(hour, minute, reminder.customDays.toSet(), reminder.intervalWeeks)
                } else if (reminder.intervalDays > 1) {
                    getNextDailyMillis(hour, minute, reminder.intervalDays)
                } else {
                    getNextDailyMillis(hour, minute, 1)
                }
            }
            "Monthly" -> getNextMonthlyMillis(hour, minute, reminder)
            else -> reminder.timeInMillis
        }
    }

    // ── Next-trigger calculations ──────────────────────────────────────

    /**
     * Calculates the next trigger time for a Weekly reminder.
     */
    fun getNextWeeklyMillis(hour: Int, minute: Int, allowedDays: Set<Int>, intervalWeeks: Int = 1): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // First, try to find a matching day in the current week
        for (i in 0..6) {
            if (i > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
            if (allowedDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                if (cal.timeInMillis > System.currentTimeMillis()) return cal.timeInMillis
            }
        }

        // If intervalWeeks > 1, jump ahead by (intervalWeeks - 1) more weeks
        if (intervalWeeks > 1) {
            cal.add(Calendar.WEEK_OF_YEAR, intervalWeeks - 1)
            // Reset to start of that week and scan again
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            for (i in 0..6) {
                if (i > 0) cal.add(Calendar.DAY_OF_YEAR, 1)
                if (allowedDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                    return cal.timeInMillis
                }
            }
        }

        return cal.timeInMillis
    }

    /**
     * Calculates the next trigger time for a Daily (every-N-days) reminder
     * that is strictly in the future.
     */
    fun getNextDailyMillis(hour: Int, minute: Int, intervalDays: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, intervalDays)
        }
        return cal.timeInMillis
    }

    /**
     * Calculates the next trigger time for a Monthly reminder.
     */
    fun getNextMonthlyMillis(hour: Int, minute: Int, reminder: Reminder): Long {
        return if (reminder.monthlyType == "day_of_month") {
            getNextMonthlyDayMillis(hour, minute, reminder.monthlyDay, reminder.intervalMonths)
        } else {
            getNextMonthlyWeekdayMillis(
                hour, minute,
                reminder.monthlyWeekOrdinal,
                reminder.monthlyDayOfWeek,
                reminder.intervalMonths
            )
        }
    }

    /**
     * Next trigger for "on day N of the month" (or last day if N=32).
     */
    private fun getNextMonthlyDayMillis(hour: Int, minute: Int, dayOfMonth: Int, intervalMonths: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Try current month first, then advance
        for (attempt in 0..24) {
            if (attempt > 0) cal.add(Calendar.MONTH, if (attempt == 1) 1 else intervalMonths - 1)

            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val targetDay = if (dayOfMonth >= Reminder.LAST_DAY_OF_MONTH) {
                maxDay
            } else {
                dayOfMonth.coerceAtMost(maxDay) // e.g. 31 → 30 in April
            }
            cal.set(Calendar.DAY_OF_MONTH, targetDay)

            if (cal.timeInMillis > System.currentTimeMillis()) return cal.timeInMillis
        }
        return cal.timeInMillis
    }

    /**
     * Next trigger for "on the Nth weekday of the month" (e.g. 2nd Tuesday, Last Friday).
     */
    private fun getNextMonthlyWeekdayMillis(
        hour: Int, minute: Int,
        weekOrdinal: Int, dayOfWeek: Int, intervalMonths: Int
    ): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (attempt in 0..24) {
            if (attempt > 0) cal.add(Calendar.MONTH, if (attempt == 1) 1 else intervalMonths - 1)

            val targetMillis = findNthWeekdayInMonth(cal, weekOrdinal, dayOfWeek, hour, minute)
            if (targetMillis > System.currentTimeMillis()) return targetMillis
        }
        return cal.timeInMillis
    }

    /**
     * Finds the Nth occurrence of a weekday in the month of [cal].
     * [ordinal] 1-4 = 1st through 4th, 5 = last occurrence.
     */
    private fun findNthWeekdayInMonth(
        cal: Calendar, ordinal: Int, dayOfWeek: Int,
        hour: Int, minute: Int
    ): Long {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        if (ordinal == Reminder.LAST_WEEK_ORDINAL) {
            // Find last occurrence: start from the last day and work backwards
            val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val search = Calendar.getInstance().apply {
                set(year, month, lastDay, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
            for (i in 0..6) {
                if (search.get(Calendar.DAY_OF_WEEK) == dayOfWeek) return search.timeInMillis
                search.add(Calendar.DAY_OF_MONTH, -1)
            }
            return search.timeInMillis
        } else {
            // Find Nth occurrence: start from day 1 and count forward
            val search = Calendar.getInstance().apply {
                set(year, month, 1, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
            var count = 0
            for (day in 1..cal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                search.set(Calendar.DAY_OF_MONTH, day)
                if (search.get(Calendar.DAY_OF_WEEK) == dayOfWeek) {
                    count++
                    if (count == ordinal) return search.timeInMillis
                }
            }
            // If ordinal > actual count (e.g. 5th Monday in a month with only 4),
            // return the last found occurrence
            return search.timeInMillis
        }
    }

    // ── Legacy compat bridge ───────────────────────────────────────────

    /**
     * Legacy method kept for source compatibility.
     * Delegates to [getNextWeeklyMillis] with intervalWeeks=1.
     */
    fun getNextDayMillis(hour: Int, minute: Int, allowedDays: Set<Int>): Long =
        getNextWeeklyMillis(hour, minute, allowedDays, 1)

    // ── Private helpers ────────────────────────────────────────────────

    private fun scheduleAlarm(context: Context, requestCode: Int, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
            putExtra(AlarmReceiver.EXTRA_ID, requestCode / 100) // original reminder id
            putExtra(AlarmReceiver.EXTRA_TIME_INDEX, requestCode % 100)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
