package com.example.notify.model

/**
 * Configuration for a dashboard filter chip.
 */
data class FilterConfig(
    val id: String,
    val label: String,
    val description: String,
    val enabled: Boolean = true
) {
    companion object {
        val DEFAULT_FILTERS = listOf(
            FilterConfig("today", "Today", "Notifications scheduled to trigger today", true),
            FilterConfig("upcoming", "Upcoming", "Notifications triggering within the next 1 hour", true),
            FilterConfig("repeating", "Repeating", "All recurring notification rules", true),
            FilterConfig("weekly", "Weekly", "Weekly and specific-day notifications", true),
            FilterConfig("monthly", "Monthly", "Monthly notifications", true)
        )
    }
}
