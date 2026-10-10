package com.example.notify.data

import android.content.Context
import com.example.notify.model.Reminder
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles persistence of reminders and app settings using SharedPreferences.
 */
object ReminderStorage {

    private const val PREFS_NAME = "notify_prefs"
    private const val KEY_REMINDERS = "reminders_list"
    private const val KEY_SNOOZE = "global_snooze"
    private const val KEY_THEME = "app_theme"
    private const val KEY_AUTO_CLEAR = "auto_clear_hours"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Snooze Duration ---

    fun getSnoozeMins(context: Context): Int =
        prefs(context).getInt(KEY_SNOOZE, 10)

    fun setSnoozeMins(context: Context, mins: Int) {
        prefs(context).edit().putInt(KEY_SNOOZE, mins).apply()
    }

    // --- Theme ---

    fun getTheme(context: Context): Int =
        prefs(context).getInt(KEY_THEME, 0)

    fun setTheme(context: Context, theme: Int) {
        prefs(context).edit().putInt(KEY_THEME, theme).apply()
    }

    // --- Auto-Clear ---

    fun getAutoClearHours(context: Context): Int =
        prefs(context).getInt(KEY_AUTO_CLEAR, 48)

    fun setAutoClearHours(context: Context, hours: Int) {
        prefs(context).edit().putInt(KEY_AUTO_CLEAR, hours).apply()
    }

    // --- Reminder CRUD ---

    fun getAllReminders(context: Context): List<Reminder> {
        val jsonStr = prefs(context).getString(KEY_REMINDERS, "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val list = mutableListOf<Reminder>()
        for (i in 0 until array.length()) {
            list.add(fromJson(array.getJSONObject(i)))
        }
        return list.sortedBy { it.timeInMillis }
    }

    fun saveReminder(context: Context, reminder: Reminder) {
        val list = getAllReminders(context).toMutableList()
        list.removeAll { it.id == reminder.id }
        list.add(reminder)
        saveList(context, list)
    }

    fun deleteReminder(context: Context, id: Int) {
        val list = getAllReminders(context).toMutableList()
        list.removeAll { it.id == id }
        saveList(context, list)
    }

    fun clearAllCompleted(context: Context) {
        val list = getAllReminders(context).filter { !it.isCompleted }
        saveList(context, list)
    }

    fun clearAllSnoozed(context: Context) {
        val list = getAllReminders(context).filter { !it.isSnoozed }
        saveList(context, list)
    }

    /**
     * Removes completed reminders older than the auto-clear threshold.
     * If auto-clear is disabled (0 hours), removes all completed reminders.
     */
    fun pruneOldCompleted(context: Context) {
        val hours = getAutoClearHours(context)
        if (hours <= 0) {
            val list = getAllReminders(context).filter { !it.isCompleted }
            saveList(context, list)
            return
        }
        val cutoff = System.currentTimeMillis() - (hours * 60L * 60L * 1000L)
        val list = getAllReminders(context)
        val filtered = list.filter { !it.isCompleted || it.timeInMillis > cutoff }
        if (list.size != filtered.size) saveList(context, filtered)
    }

    // --- JSON serialization ---

    private fun fromJson(obj: JSONObject): Reminder {
        val daysArray = obj.optJSONArray("customDays") ?: JSONArray()
        val daysList = List(daysArray.length()) { daysArray.getInt(it) }

        val extraTimesArray = obj.optJSONArray("extraTimes") ?: JSONArray()
        val extraTimesList = List(extraTimesArray.length()) { extraTimesArray.getString(it) }

        // Backward-compat: migrate old "Daily" → "Daily" with intervalDays=1,
        // old "Specific Days" → "Weekly" with intervalWeeks=1
        val rawFreq = obj.optString("frequency", "One-Time")
        val frequency = when (rawFreq) {
            "Specific Days" -> "Weekly"
            else -> rawFreq
        }

        return Reminder(
            id = obj.getInt("id"),
            title = obj.getString("title"),
            desc = obj.optString("desc", ""),
            timeInMillis = obj.getLong("timeInMillis"),
            frequency = frequency,
            customDays = daysList,
            isCompleted = obj.optBoolean("isCompleted", false),
            isSnoozed = obj.optBoolean("isSnoozed", false),
            intervalDays = obj.optInt("intervalDays", 1),
            intervalWeeks = obj.optInt("intervalWeeks", 1),
            monthlyType = obj.optString("monthlyType", "day_of_month"),
            monthlyDay = obj.optInt("monthlyDay", 1),
            monthlyWeekOrdinal = obj.optInt("monthlyWeekOrdinal", 1),
            monthlyDayOfWeek = obj.optInt("monthlyDayOfWeek", 2),
            intervalMonths = obj.optInt("intervalMonths", 1),
            extraTimes = extraTimesList
        )
    }

    private fun toJson(r: Reminder): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("title", r.title)
        put("desc", r.desc)
        put("timeInMillis", r.timeInMillis)
        put("frequency", r.frequency)
        put("isCompleted", r.isCompleted)
        put("isSnoozed", r.isSnoozed)
        put("intervalDays", r.intervalDays)
        put("intervalWeeks", r.intervalWeeks)
        put("monthlyType", r.monthlyType)
        put("monthlyDay", r.monthlyDay)
        put("monthlyWeekOrdinal", r.monthlyWeekOrdinal)
        put("monthlyDayOfWeek", r.monthlyDayOfWeek)
        put("intervalMonths", r.intervalMonths)

        val daysArr = JSONArray()
        r.customDays.forEach { daysArr.put(it) }
        put("customDays", daysArr)

        val timesArr = JSONArray()
        r.extraTimes.forEach { timesArr.put(it) }
        put("extraTimes", timesArr)
    }

    private fun saveList(context: Context, list: List<Reminder>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        prefs(context).edit().putString(KEY_REMINDERS, array.toString()).apply()
    }
}
