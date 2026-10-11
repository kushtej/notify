package com.example.notify

import com.example.notify.model.Reminder
import com.example.notify.scheduler.AlarmScheduler
import com.example.notify.ui.FrequencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FrequencyTest {

    @Test
    fun testDailyIntervalCalculation() {
        val next1 = AlarmScheduler.getNextDailyMillis(14, 0, 1)
        val cal1 = Calendar.getInstance().apply { timeInMillis = next1 }
        assertEquals(14, cal1.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal1.get(Calendar.MINUTE))
        assertTrue(next1 > System.currentTimeMillis())

        val next2 = AlarmScheduler.getNextDailyMillis(14, 0, 2)
        val cal2 = Calendar.getInstance().apply { timeInMillis = next2 }
        assertEquals(14, cal2.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal2.get(Calendar.MINUTE))
        assertTrue(next2 > System.currentTimeMillis())
    }

    @Test
    fun testWeeklyIntervalSingleDay() {
        // Schedule every other Tuesday (interval = 2)
        val next = AlarmScheduler.getNextWeeklyMillis(10, 0, setOf(Calendar.TUESDAY), intervalWeeks = 2)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.TUESDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertTrue(next > System.currentTimeMillis())
    }

    @Test
    fun testWeeklyMultipleDays() {
        // Schedule Mon, Wed, Fri
        val days = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
        val next = AlarmScheduler.getNextWeeklyMillis(9, 30, days, intervalWeeks = 1)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertTrue(days.contains(cal.get(Calendar.DAY_OF_WEEK)))
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertTrue(next > System.currentTimeMillis())
    }

    @Test
    fun testMonthlyDayOfMonth() {
        // 1st of month (e.g. rent)
        val reminderRent = Reminder(
            id = 1,
            title = "Pay Rent",
            desc = "",
            timeInMillis = 0,
            frequency = "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 1,
            intervalMonths = 1
        )
        val nextRent = AlarmScheduler.getNextMonthlyMillis(9, 0, reminderRent)
        val calRent = Calendar.getInstance().apply { timeInMillis = nextRent }
        assertEquals(1, calRent.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, calRent.get(Calendar.HOUR_OF_DAY))
        assertTrue(nextRent > System.currentTimeMillis())

        // 10th of month (e.g. Netflix)
        val reminderNetflix = Reminder(
            id = 2,
            title = "Netflix",
            desc = "",
            timeInMillis = 0,
            frequency = "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 10,
            intervalMonths = 1
        )
        val nextNetflix = AlarmScheduler.getNextMonthlyMillis(12, 0, reminderNetflix)
        val calNetflix = Calendar.getInstance().apply { timeInMillis = nextNetflix }
        assertEquals(10, calNetflix.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, calNetflix.get(Calendar.HOUR_OF_DAY))
        assertTrue(nextNetflix > System.currentTimeMillis())
    }

    @Test
    fun testMonthlyLastDayOfMonth() {
        // Last day of month (sentinel = 32)
        val reminderLastDay = Reminder(
            id = 3,
            title = "Credit Card Settlement",
            desc = "",
            timeInMillis = 0,
            frequency = "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = Reminder.LAST_DAY_OF_MONTH,
            intervalMonths = 1
        )
        val next = AlarmScheduler.getNextMonthlyMillis(18, 0, reminderLastDay)
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        assertEquals(maxDay, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
        assertTrue(next > System.currentTimeMillis())
    }

    @Test
    fun testMonthlyOrdinalWeekday() {
        // 2nd Tuesday of month (e.g. Sprint end)
        val reminderSprint = Reminder(
            id = 4,
            title = "Sprint End",
            desc = "",
            timeInMillis = 0,
            frequency = "Monthly",
            monthlyType = "day_of_week",
            monthlyWeekOrdinal = 2,
            monthlyDayOfWeek = Calendar.TUESDAY,
            intervalMonths = 1
        )
        val nextSprint = AlarmScheduler.getNextMonthlyMillis(17, 0, reminderSprint)
        val calSprint = Calendar.getInstance().apply { timeInMillis = nextSprint }
        assertEquals(Calendar.TUESDAY, calSprint.get(Calendar.DAY_OF_WEEK))
        assertEquals(2, calSprint.get(Calendar.DAY_OF_WEEK_IN_MONTH))
        assertEquals(17, calSprint.get(Calendar.HOUR_OF_DAY))
        assertTrue(nextSprint > System.currentTimeMillis())

        // Last Friday of month (e.g. Retro)
        val reminderRetro = Reminder(
            id = 5,
            title = "Team Retro",
            desc = "",
            timeInMillis = 0,
            frequency = "Monthly",
            monthlyType = "day_of_week",
            monthlyWeekOrdinal = Reminder.LAST_WEEK_ORDINAL,
            monthlyDayOfWeek = Calendar.FRIDAY,
            intervalMonths = 1
        )
        val nextRetro = AlarmScheduler.getNextMonthlyMillis(16, 0, reminderRetro)
        val calRetro = Calendar.getInstance().apply { timeInMillis = nextRetro }
        assertEquals(Calendar.FRIDAY, calRetro.get(Calendar.DAY_OF_WEEK))
        // Verify it is indeed the last Friday of that month
        val checkNextWeek = (calRetro.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
        assertTrue(checkNextWeek.get(Calendar.MONTH) != calRetro.get(Calendar.MONTH))
        assertTrue(nextRetro > System.currentTimeMillis())
    }

    @Test
    fun testFrequencyFormatterOutputs() {
        // Daily (all 7 days or default)
        val daily1 = Reminder(1, "Daily", "", 0, "Daily", customDays = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        ))
        assertEquals("Every day", FrequencyFormatter.formatFrequency(daily1))

        // Mon, Tue, Wed
        val monTueWed = Reminder(
            2, "Work Days", "", 0, "Specific Days",
            customDays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY)
        )
        assertEquals("Every week on Mon, Tue, Wed", FrequencyFormatter.formatFrequency(monTueWed))

        // Weekdays with brackets
        val weekdays = Reminder(
            3, "Weekdays", "", 0, "Specific Days",
            customDays = listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY
            )
        )
        assertEquals("Every weekday (Monday to Friday)", FrequencyFormatter.formatFrequency(weekdays))

        // Weekends with brackets
        val weekends = Reminder(
            4, "Weekends", "", 0, "Specific Days",
            customDays = listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        )
        assertEquals("Every weekend (Saturday, Sunday)", FrequencyFormatter.formatFrequency(weekends))

        // Every other Tuesday
        val biweeklyTue = Reminder(
            5, "Sprint Planning", "", 0, "Specific Days",
            customDays = listOf(Calendar.TUESDAY),
            intervalWeeks = 2
        )
        assertEquals("Every other Tuesday", FrequencyFormatter.formatFrequency(biweeklyTue))

        // Every 3rd week on Mon
        val triweeklyMon = Reminder(
            6, "Client Sync", "", 0, "Specific Days",
            customDays = listOf(Calendar.MONDAY),
            intervalWeeks = 3
        )
        assertEquals("Every 3rd week on Mon", FrequencyFormatter.formatFrequency(triweeklyMon))

        // Monthly Day of Month: Rent on 1st
        val rent = Reminder(
            7, "Rent", "", 0, "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 1,
            intervalMonths = 1
        )
        assertEquals("Every month on the 1st", FrequencyFormatter.formatFrequency(rent))

        // Monthly Last Day
        val lastDay = Reminder(
            8, "Last Day", "", 0, "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = Reminder.LAST_DAY_OF_MONTH,
            intervalMonths = 1
        )
        assertEquals("Every month on the last day", FrequencyFormatter.formatFrequency(lastDay))

        // Monthly Ordinal Weekday: 2nd Tuesday
        val sprint = Reminder(
            9, "Sprint", "", 0, "Monthly",
            monthlyType = "day_of_week",
            monthlyWeekOrdinal = 2,
            monthlyDayOfWeek = Calendar.TUESDAY,
            intervalMonths = 1
        )
        assertEquals("Every month on the 2nd Tuesday", FrequencyFormatter.formatFrequency(sprint))

        // Every 2nd month
        val biMonthly = Reminder(
            10, "Bimonthly Review", "", 0, "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 1,
            intervalMonths = 2
        )
        assertEquals("Every 2nd month on the 1st", FrequencyFormatter.formatFrequency(biMonthly))

        // Every 4th month
        val fourMonthly = Reminder(
            11, "Quad Monthly", "", 0, "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 10,
            intervalMonths = 4
        )
        assertEquals("Every 4th month on the 10th", FrequencyFormatter.formatFrequency(fourMonthly))

        // Every 6th month
        val semiAnnual = Reminder(
            12, "Semi Annual", "", 0, "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 15,
            intervalMonths = 6
        )
        assertEquals("Every 6th month on the 15th", FrequencyFormatter.formatFrequency(semiAnnual))

        // Every year
        val yearly = Reminder(
            13, "Annual Subscription", "", 0, "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 1,
            intervalMonths = 12
        )
        assertEquals("Every year on the 1st", FrequencyFormatter.formatFrequency(yearly))
    }

    @Test
    fun testDetailedExplanations() {
        val daysMonWed = setOf(Calendar.MONDAY, Calendar.WEDNESDAY)

        // Every other week on Monday and Wednesday at 9:00 AM
        val exp1 = FrequencyFormatter.getDaysScheduleExplanation(daysMonWed, 2, 9, 0, emptyList())
        assertEquals("Notification will trigger every other week on Monday and Wednesday at 9:00 AM.", exp1)

        // Every 3rd week on Monday and Wednesday at 9:00 AM
        val exp2 = FrequencyFormatter.getDaysScheduleExplanation(daysMonWed, 3, 9, 0, emptyList())
        assertEquals("Notification will trigger every 3rd week on Monday and Wednesday at 9:00 AM.", exp2)

        // Weekdays at 9:00 AM
        val weekdays = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        )
        val expWeekdays = FrequencyFormatter.getDaysScheduleExplanation(weekdays, 1, 9, 0, emptyList())
        assertEquals("Notification will trigger every weekday (Monday to Friday) at 9:00 AM.", expWeekdays)

        // Weekends at 9:00 AM
        val weekends = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
        val expWeekends = FrequencyFormatter.getDaysScheduleExplanation(weekends, 1, 9, 0, emptyList())
        assertEquals("Notification will trigger every weekend (Saturday, Sunday) at 9:00 AM.", expWeekends)

        // Every day at 9:00 AM
        val allDays = weekdays + weekends
        val expAll = FrequencyFormatter.getDaysScheduleExplanation(allDays, 1, 9, 0, emptyList())
        assertEquals("Notification will trigger every day at 9:00 AM.", expAll)

        // Monthly 2nd month
        val expMonth2 = FrequencyFormatter.getMonthlyScheduleExplanation("day_of_month", 1, 1, 2, 2, 9, 0, emptyList())
        assertEquals("Notification will trigger on the 1st of every 2nd month at 9:00 AM.", expMonth2)

        // Monthly 4th month
        val expMonth4 = FrequencyFormatter.getMonthlyScheduleExplanation("day_of_month", 1, 1, 2, 4, 9, 0, emptyList())
        assertEquals("Notification will trigger on the 1st of every 4th month at 9:00 AM.", expMonth4)

        // Monthly 6th month
        val expMonth6 = FrequencyFormatter.getMonthlyScheduleExplanation("day_of_month", 1, 1, 2, 6, 9, 0, emptyList())
        assertEquals("Notification will trigger on the 1st of every 6th month at 9:00 AM.", expMonth6)
    }

    @Test
    fun testFormatAllTimes() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }
        val extra = listOf("14:30", "20:15")
        val formatted = FrequencyFormatter.formatAllTimes(cal.timeInMillis, extra)
        assertEquals("9:00 AM, 2:30 PM, 8:15 PM", formatted)
    }

    @Test
    fun testCountRemindersInJson() {
        val rawArray = """[{"id":1,"title":"Test 1"},{"id":2,"title":"Test 2"}]"""
        assertEquals(2, com.example.notify.data.ReminderStorage.countRemindersInJson(rawArray))

        val wrappedObj = """{"app":"Notify","count":1,"reminders":[{"id":1,"title":"Wrapped"}]}"""
        assertEquals(1, com.example.notify.data.ReminderStorage.countRemindersInJson(wrappedObj))

        val emptyJson = "[]"
        assertEquals(0, com.example.notify.data.ReminderStorage.countRemindersInJson(emptyJson))

        val invalid = "not a json"
        assertEquals(0, com.example.notify.data.ReminderStorage.countRemindersInJson(invalid))
    }

    @Test
    fun testTriggersOnDate() {
        val testCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 10)
        }

        // Daily reminder always triggers on any date
        val daily = Reminder(id = 1, title = "Daily", desc = "", timeInMillis = 0, frequency = "Daily")
        assertTrue(com.example.notify.ui.screens.triggersOnDate(daily, testCal))

        // Weekly reminder with Saturday triggers on Saturday Oct 10, 2026 (Oct 10, 2026 is a Saturday!)
        val weeklySat = Reminder(
            id = 2, title = "Weekly Sat", desc = "", timeInMillis = 0,
            frequency = "Weekly", customDays = listOf(Calendar.SATURDAY)
        )
        assertTrue(com.example.notify.ui.screens.triggersOnDate(weeklySat, testCal))

        // Weekly reminder with Monday does not trigger on Saturday
        val weeklyMon = Reminder(
            id = 3, title = "Weekly Mon", desc = "", timeInMillis = 0,
            frequency = "Weekly", customDays = listOf(Calendar.MONDAY)
        )
        org.junit.Assert.assertFalse(com.example.notify.ui.screens.triggersOnDate(weeklyMon, testCal))

        // One-time for today
        val oneTimeToday = Reminder(
            id = 4, title = "One Time Today", desc = "",
            timeInMillis = testCal.timeInMillis, frequency = "One-Time"
        )
        assertTrue(com.example.notify.ui.screens.triggersOnDate(oneTimeToday, testCal))

        // One-time for tomorrow
        val futureCal = (testCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }
        val oneTimeFuture = Reminder(
            id = 5, title = "One Time Future", desc = "",
            timeInMillis = futureCal.timeInMillis, frequency = "One-Time"
        )
        org.junit.Assert.assertFalse(com.example.notify.ui.screens.triggersOnDate(oneTimeFuture, testCal))
        assertTrue(com.example.notify.ui.screens.isUpcomingOneTime(oneTimeFuture, testCal))
    }

    @Test
    fun testTriggersWithinOneHour() {
        val now = 1000000000000L

        // Triggering in 30 minutes (1800000 ms) -> within 1 hour
        val in30Mins = Reminder(
            id = 10, title = "Soon", desc = "",
            timeInMillis = now + 1800000L, frequency = "One-Time"
        )
        assertTrue(com.example.notify.ui.screens.triggersWithinOneHour(in30Mins, now))

        // Triggering in 2 hours (7200000 ms) -> NOT within 1 hour
        val in2Hours = Reminder(
            id = 11, title = "Later", desc = "",
            timeInMillis = now + 7200000L, frequency = "One-Time"
        )
        org.junit.Assert.assertFalse(com.example.notify.ui.screens.triggersWithinOneHour(in2Hours, now))

        // Triggering in the past -> NOT within 1 hour
        val inPast = Reminder(
            id = 12, title = "Past", desc = "",
            timeInMillis = now - 5000L, frequency = "One-Time"
        )
        org.junit.Assert.assertFalse(com.example.notify.ui.screens.triggersWithinOneHour(inPast, now))
    }

    @Test
    fun testWeeklyIntervalStartingWeekAfterNext() {
        val now = System.currentTimeMillis()
        val currentMonday = AlarmScheduler.getStartOfWeek(now)

        // Target: Every other Tuesday (interval = 2)
        // Set start week to week after next (2 weeks ahead)
        val weekAfterNextMonday = (currentMonday.clone() as Calendar).apply {
            add(Calendar.WEEK_OF_YEAR, 2)
        }

        val firstTrigger = AlarmScheduler.getNextWeeklyMillis(
            hour = 10,
            minute = 0,
            allowedDays = setOf(Calendar.TUESDAY),
            intervalWeeks = 2,
            startMillis = weekAfterNextMonday.timeInMillis,
            fromMillis = now
        )

        val cal1 = Calendar.getInstance().apply { timeInMillis = firstTrigger }
        assertEquals(Calendar.TUESDAY, cal1.get(Calendar.DAY_OF_WEEK))
        assertEquals(10, cal1.get(Calendar.HOUR_OF_DAY))
        val diffWeeks1 = ((AlarmScheduler.getStartOfWeek(firstTrigger).timeInMillis - currentMonday.timeInMillis) / (7L * 24 * 3600 * 1000)).toInt()
        assertEquals(2, diffWeeks1)

        // Next occurrence after firstTrigger
        val secondTrigger = AlarmScheduler.getNextWeeklyMillis(
            hour = 10,
            minute = 0,
            allowedDays = setOf(Calendar.TUESDAY),
            intervalWeeks = 2,
            startMillis = weekAfterNextMonday.timeInMillis,
            fromMillis = firstTrigger
        )
        val diffWeeks2 = ((AlarmScheduler.getStartOfWeek(secondTrigger).timeInMillis - currentMonday.timeInMillis) / (7L * 24 * 3600 * 1000)).toInt()
        assertEquals(4, diffWeeks2) // Exactly 2 weeks after week 2 = week 4! Week 3 skipped!
    }

    @Test
    fun testWeeklyMultipleDaysAlternatingWeeks() {
        val now = System.currentTimeMillis()
        val currentMonday = AlarmScheduler.getStartOfWeek(now)

        // Repeat every other week on Tuesday & Thursday starting next week (week 1)
        val nextMonday = (currentMonday.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 1) }

        val trigger1 = AlarmScheduler.getNextWeeklyMillis(
            hour = 9,
            minute = 0,
            allowedDays = setOf(Calendar.TUESDAY, Calendar.THURSDAY),
            intervalWeeks = 2,
            startMillis = nextMonday.timeInMillis,
            fromMillis = now
        )
        val cal1 = Calendar.getInstance().apply { timeInMillis = trigger1 }
        assertEquals(Calendar.TUESDAY, cal1.get(Calendar.DAY_OF_WEEK))
        val diffWeeks1 = ((AlarmScheduler.getStartOfWeek(trigger1).timeInMillis - currentMonday.timeInMillis) / (7L * 24 * 3600 * 1000)).toInt()
        assertEquals(1, diffWeeks1)

        // After Tuesday fires in week 1, Thursday should fire in the same week
        val trigger2 = AlarmScheduler.getNextWeeklyMillis(
            hour = 9,
            minute = 0,
            allowedDays = setOf(Calendar.TUESDAY, Calendar.THURSDAY),
            intervalWeeks = 2,
            startMillis = nextMonday.timeInMillis,
            fromMillis = trigger1
        )
        val cal2 = Calendar.getInstance().apply { timeInMillis = trigger2 }
        assertEquals(Calendar.THURSDAY, cal2.get(Calendar.DAY_OF_WEEK))
        val diffWeeks2 = ((AlarmScheduler.getStartOfWeek(trigger2).timeInMillis - currentMonday.timeInMillis) / (7L * 24 * 3600 * 1000)).toInt()
        assertEquals(1, diffWeeks2) // Same week!

        // After Thursday fires in week 1, week 2 is skipped and it fires Tuesday in week 3!
        val trigger3 = AlarmScheduler.getNextWeeklyMillis(
            hour = 9,
            minute = 0,
            allowedDays = setOf(Calendar.TUESDAY, Calendar.THURSDAY),
            intervalWeeks = 2,
            startMillis = nextMonday.timeInMillis,
            fromMillis = trigger2
        )
        val cal3 = Calendar.getInstance().apply { timeInMillis = trigger3 }
        assertEquals(Calendar.TUESDAY, cal3.get(Calendar.DAY_OF_WEEK))
        val diffWeeks3 = ((AlarmScheduler.getStartOfWeek(trigger3).timeInMillis - currentMonday.timeInMillis) / (7L * 24 * 3600 * 1000)).toInt()
        assertEquals(3, diffWeeks3) // Week 2 was skipped!
    }

    @Test
    fun testMonthlyIntervalStartingMonth() {
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Suppose user wants every 2nd month on the 15th, starting in December 2026
        val startCal = (baseCal.clone() as Calendar).apply {
            set(Calendar.MONTH, Calendar.DECEMBER)
        }

        val reminder = Reminder(
            id = 100,
            title = "Bimonthly Starting Dec",
            desc = "",
            timeInMillis = 0,
            frequency = "Monthly",
            monthlyType = "day_of_month",
            monthlyDay = 15,
            intervalMonths = 2,
            startMillis = startCal.timeInMillis
        )

        // First occurrence
        val trigger1 = AlarmScheduler.getNextMonthlyMillis(
            hour = 10,
            minute = 0,
            reminder = reminder,
            fromMillis = baseCal.timeInMillis
        )
        val cal1 = Calendar.getInstance().apply { timeInMillis = trigger1 }
        assertEquals(2026, cal1.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, cal1.get(Calendar.MONTH))
        assertEquals(15, cal1.get(Calendar.DAY_OF_MONTH))

        // Second occurrence (after Dec 15 fires)
        val trigger2 = AlarmScheduler.getNextMonthlyMillis(
            hour = 10,
            minute = 0,
            reminder = reminder,
            fromMillis = trigger1
        )
        val cal2 = Calendar.getInstance().apply { timeInMillis = trigger2 }
        assertEquals(2027, cal2.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, cal2.get(Calendar.MONTH)) // Dec + 2 = Feb! (Jan skipped!)
        assertEquals(15, cal2.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testSelectableDateWeekdays() {
        // Test that when Sunday is selected, only Sundays from today onwards are selectable
        val customDays = setOf(Calendar.SUNDAY)
        val now = Calendar.getInstance()
        val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }

        fun isSelectable(utcMillis: Long): Boolean {
            if (utcMillis < todayUtc.timeInMillis) return false
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
            return customDays.contains(utcCal.get(Calendar.DAY_OF_WEEK))
        }

        // Past date is disabled
        val yesterdayUtc = (todayUtc.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
        assertFalse(isSelectable(yesterdayUtc.timeInMillis))

        // Check the next 14 days
        for (i in 0..14) {
            val futureUtc = (todayUtc.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            val dayOfWeek = futureUtc.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SUNDAY) {
                assertTrue("Sunday should be selectable", isSelectable(futureUtc.timeInMillis))
            } else {
                assertFalse("Non-Sunday should be disabled", isSelectable(futureUtc.timeInMillis))
            }
        }
    }

    @Test
    fun testSelectableDateMonthly() {
        val now = Calendar.getInstance()
        val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }

        // Test monthly day 20
        fun isSelectableDay20(utcMillis: Long): Boolean {
            if (utcMillis < todayUtc.timeInMillis) return false
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
            val maxDay = utcCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val target = 20.coerceAtMost(maxDay)
            return utcCal.get(Calendar.DAY_OF_MONTH) == target
        }

        for (i in 0..60) {
            val testUtc = (todayUtc.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            if (testUtc.get(Calendar.DAY_OF_MONTH) == 20) {
                assertTrue(isSelectableDay20(testUtc.timeInMillis))
            } else {
                assertFalse(isSelectableDay20(testUtc.timeInMillis))
            }
        }
    }
}
