package com.example.notify

import com.example.notify.model.Reminder
import com.example.notify.scheduler.AlarmScheduler
import com.example.notify.ui.FrequencyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

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
}
