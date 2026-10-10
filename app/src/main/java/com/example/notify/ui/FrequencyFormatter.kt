package com.example.notify.ui

import com.example.notify.model.Reminder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility to format reminder recurrence and multiple time settings
 * into natural, grammatically correct English for UI display and detailed info banners.
 */
object FrequencyFormatter {

    private val ALL_WEEKDAYS = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )
    private val ALL_WEEKENDS = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
    private val ALL_7_DAYS = ALL_WEEKDAYS + ALL_WEEKENDS

    fun formatWeekCycle(intervalWeeks: Int): String {
        return when (intervalWeeks) {
            1 -> "Every week"
            2 -> "Every other week"
            3 -> "Every 3rd week"
            else -> "Every ${formatOrdinal(intervalWeeks)} week"
        }
    }

    fun formatWeekCycleSentence(intervalWeeks: Int): String {
        return when (intervalWeeks) {
            1 -> "every week"
            2 -> "every other week"
            3 -> "every 3rd week"
            else -> "every ${formatOrdinal(intervalWeeks)} week"
        }
    }

    fun formatMonthCycle(intervalMonths: Int): String {
        return when (intervalMonths) {
            1 -> "Every month"
            2 -> "Every 2nd month"
            3 -> "Every 3rd month"
            4 -> "Every 4th month"
            6 -> "Every 6th month"
            12 -> "Every year"
            else -> "Every ${formatOrdinal(intervalMonths)} month"
        }
    }

    fun formatMonthCycleSentence(intervalMonths: Int): String {
        return when (intervalMonths) {
            1 -> "every month"
            2 -> "every 2nd month"
            3 -> "every 3rd month"
            4 -> "every 4th month"
            6 -> "every 6th month"
            12 -> "once a year"
            else -> "every ${formatOrdinal(intervalMonths)} month"
        }
    }

    fun formatFrequency(reminder: Reminder): String {
        return when (reminder.frequency) {
            "One-Time" -> "One-Time"

            "Daily", "Specific Days", "Weekly" -> {
                val daysSet = reminder.customDays.toSet()
                val intervalWeeks = reminder.intervalWeeks
                val weekCycle = formatWeekCycle(intervalWeeks)

                when {
                    daysSet.isEmpty() || daysSet.containsAll(ALL_7_DAYS) -> {
                        if (intervalWeeks <= 1) "Every day"
                        else "$weekCycle (All days)"
                    }
                    daysSet == ALL_WEEKDAYS -> {
                        if (intervalWeeks <= 1) "Every weekday (Monday to Friday)"
                        else "$weekCycle on weekdays (Monday to Friday)"
                    }
                    daysSet == ALL_WEEKENDS -> {
                        if (intervalWeeks <= 1) "Every weekend (Saturday, Sunday)"
                        else "$weekCycle on weekends (Saturday, Sunday)"
                    }
                    daysSet.size == 1 && intervalWeeks == 2 -> {
                        val dayName = getFullDayName(daysSet.first())
                        "Every other $dayName"
                    }
                    else -> {
                        val orderedDays = listOf(
                            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
                        ).filter { daysSet.contains(it) }.map { getShortDayName(it) }

                        val daysString = orderedDays.joinToString(", ")
                        "$weekCycle on $daysString"
                    }
                }
            }

            "Monthly" -> {
                val pattern = if (reminder.monthlyType == "day_of_month") {
                    if (reminder.monthlyDay == Reminder.LAST_DAY_OF_MONTH) "the last day"
                    else "the ${formatOrdinal(reminder.monthlyDay)}"
                } else {
                    val ordinal = when (reminder.monthlyWeekOrdinal) {
                        1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; 4 -> "4th"
                        Reminder.LAST_WEEK_ORDINAL -> "last"
                        else -> "${reminder.monthlyWeekOrdinal}th"
                    }
                    val dayName = getFullDayName(reminder.monthlyDayOfWeek)
                    "the $ordinal $dayName"
                }

                val monthCycle = formatMonthCycle(reminder.intervalMonths)
                "$monthCycle on $pattern"
            }

            else -> reminder.frequency
        }
    }

    fun formatAllTimes(primaryTimeMillis: Long, extraTimes: List<String>): String {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance().apply { timeInMillis = primaryTimeMillis }
        list.add(formatHourMinute(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
        extraTimes.forEach { extra ->
            val parts = extra.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                list.add(formatHourMinute(h, m))
            }
        }
        return list.joinToString(", ")
    }

    fun formatTimesDescription(primaryHour: Int, primaryMinute: Int, extraTimes: List<String>): String {
        val times = mutableListOf<String>()
        if (primaryHour in 0..23 && primaryMinute in 0..59) {
            times.add(formatHourMinute(primaryHour, primaryMinute))
        }
        extraTimes.forEach { extra ->
            val parts = extra.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                times.add(formatHourMinute(h, m))
            }
        }
        return when (times.size) {
            0 -> ""
            1 -> "at ${times[0]}"
            2 -> "at ${times[0]} and ${times[1]}"
            else -> "at ${times.dropLast(1).joinToString(", ")}, and ${times.last()}"
        }
    }

    fun getDaysScheduleExplanation(
        selectedDays: Set<Int>,
        intervalWeeks: Int,
        primaryHour: Int,
        primaryMinute: Int,
        extraTimes: List<String>
    ): String {
        val timesDesc = formatTimesDescription(primaryHour, primaryMinute, extraTimes)

        if (timesDesc.isEmpty()) {
            return "Please add at least one notification time."
        }

        if (selectedDays.isEmpty()) {
            return "Please select at least one day for the notification to trigger."
        }

        val weekCycleSentence = formatWeekCycleSentence(intervalWeeks)

        return when {
            selectedDays.containsAll(ALL_7_DAYS) -> {
                if (intervalWeeks <= 1) "Notification will trigger every day $timesDesc."
                else "Notification will trigger $weekCycleSentence across all days $timesDesc."
            }
            selectedDays == ALL_WEEKDAYS -> {
                if (intervalWeeks <= 1) "Notification will trigger every weekday (Monday to Friday) $timesDesc."
                else "Notification will trigger $weekCycleSentence on weekdays (Monday to Friday) $timesDesc."
            }
            selectedDays == ALL_WEEKENDS -> {
                if (intervalWeeks <= 1) "Notification will trigger every weekend (Saturday, Sunday) $timesDesc."
                else "Notification will trigger $weekCycleSentence on weekends (Saturday, Sunday) $timesDesc."
            }
            selectedDays.size == 1 -> {
                val dayName = getFullDayName(selectedDays.first())
                "Notification will trigger $weekCycleSentence on $dayName $timesDesc."
            }
            else -> {
                val ordered = listOf(
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
                ).filter { selectedDays.contains(it) }.map { getFullDayName(it) }

                val formattedDays = when (ordered.size) {
                    2 -> "${ordered[0]} and ${ordered[1]}"
                    else -> "${ordered.dropLast(1).joinToString(", ")}, and ${ordered.last()}"
                }

                "Notification will trigger $weekCycleSentence on $formattedDays $timesDesc."
            }
        }
    }

    fun getMonthlyScheduleExplanation(
        monthlyType: String,
        monthlyDay: Int,
        weekOrdinal: Int,
        dayOfWeek: Int,
        intervalMonths: Int,
        primaryHour: Int,
        primaryMinute: Int,
        extraTimes: List<String>
    ): String {
        val timesDesc = formatTimesDescription(primaryHour, primaryMinute, extraTimes)

        if (timesDesc.isEmpty()) {
            return "Please add at least one notification time."
        }

        val cycle = formatMonthCycleSentence(intervalMonths)

        return if (monthlyType == "day_of_month") {
            if (monthlyDay == Reminder.LAST_DAY_OF_MONTH) {
                if (intervalMonths == 12) {
                    "Notification will trigger once a year on the last day of the month $timesDesc."
                } else {
                    "Notification will trigger on the last day of $cycle (automatically adjusting for 28, 29, 30, or 31 days) $timesDesc."
                }
            } else if (monthlyDay == 31) {
                if (intervalMonths == 12) {
                    "Notification will trigger once a year on the 31st (or the last day in shorter months) $timesDesc."
                } else {
                    "Notification will trigger on the 31st of $cycle (or the last day in shorter months) $timesDesc."
                }
            } else {
                if (intervalMonths == 12) {
                    "Notification will trigger once a year on the ${formatOrdinal(monthlyDay)} $timesDesc."
                } else {
                    "Notification will trigger on the ${formatOrdinal(monthlyDay)} of $cycle $timesDesc."
                }
            }
        } else {
            val ord = when (weekOrdinal) {
                1 -> "first"
                2 -> "second"
                3 -> "third"
                4 -> "fourth"
                else -> "last"
            }
            val dayName = getFullDayName(dayOfWeek)
            if (intervalMonths == 12) {
                "Notification will trigger once a year on the $ord $dayName $timesDesc."
            } else {
                "Notification will trigger on the $ord $dayName of $cycle $timesDesc."
            }
        }
    }

    fun getOneTimeExplanation(
        dateMillis: Long,
        primaryHour: Int,
        primaryMinute: Int,
        extraTimes: List<String>
    ): String {
        val timesDesc = formatTimesDescription(primaryHour, primaryMinute, extraTimes)
        if (timesDesc.isEmpty()) {
            return "Please add at least one notification time."
        }
        val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(dateMillis))
        return "Notification will trigger once on $dateStr $timesDesc."
    }

    private fun formatHourMinute(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayH = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%d:%02d %s", displayH, minute, amPm)
    }

    fun formatOrdinal(n: Int): String {
        if (n in 11..13) return "${n}th"
        return when (n % 10) {
            1 -> "${n}st"
            2 -> "${n}nd"
            3 -> "${n}rd"
            else -> "${n}th"
        }
    }

    fun getShortDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        Calendar.SUNDAY -> "Sun"
        else -> ""
    }

    fun getFullDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.MONDAY -> "Monday"
        Calendar.TUESDAY -> "Tuesday"
        Calendar.WEDNESDAY -> "Wednesday"
        Calendar.THURSDAY -> "Thursday"
        Calendar.FRIDAY -> "Friday"
        Calendar.SATURDAY -> "Saturday"
        Calendar.SUNDAY -> "Sunday"
        else -> ""
    }
}
