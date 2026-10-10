package com.example.notify.model

/**
 * Represents a reminder with scheduling and state information.
 *
 * Frequency types:
 * - "One-Time"  : Fires once on the date encoded in [timeInMillis].
 * - "Daily"     : Fires every [intervalDays] days.
 * - "Weekly"    : Fires on selected [customDays] every [intervalWeeks] weeks.
 * - "Monthly"   : Fires monthly based on [monthlyType]:
 *       "day_of_month" → on [monthlyDay] (1-31, or 32 = last day).
 *       "day_of_week"  → on the [monthlyWeekOrdinal] occurrence of
 *                         [monthlyDayOfWeek] (5 = last).
 *                Repeats every [intervalMonths] months.
 *
 * Multiple times per day:
 * - [timeInMillis] always holds the next trigger time (primary time).
 * - [extraTimes] holds additional "HH:mm" times beyond the primary one.
 *   The scheduler creates sub-alarms for each extra time.
 */
data class Reminder(
    val id: Int,
    val title: String,
    val desc: String,
    val timeInMillis: Long,
    val frequency: String,
    val customDays: List<Int> = emptyList(),
    val isCompleted: Boolean = false,
    val isSnoozed: Boolean = false,

    // Daily: repeat every N days (1 = every day, 2 = every other day, …)
    val intervalDays: Int = 1,

    // Weekly: repeat every N weeks
    val intervalWeeks: Int = 1,

    // Monthly: which kind of monthly pattern
    val monthlyType: String = "day_of_month",  // "day_of_month" | "day_of_week"
    val monthlyDay: Int = 1,                   // 1-31, or 32 = "Last Day"
    val monthlyWeekOrdinal: Int = 1,           // 1-4 = 1st-4th, 5 = Last
    val monthlyDayOfWeek: Int = 2,             // Calendar.MONDAY (2) default
    val intervalMonths: Int = 1,               // repeat every N months

    // Multiple times per day: additional "HH:mm" strings
    val extraTimes: List<String> = emptyList()
) {
    companion object {
        /** Sentinel value for [monthlyDay] meaning "last day of the month". */
        const val LAST_DAY_OF_MONTH = 32

        /** Sentinel value for [monthlyWeekOrdinal] meaning "last occurrence". */
        const val LAST_WEEK_ORDINAL = 5
    }
}
