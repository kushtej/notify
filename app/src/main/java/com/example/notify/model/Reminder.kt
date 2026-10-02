package com.example.notify.model

/**
 * Represents a reminder with scheduling and state information.
 */
data class Reminder(
    val id: Int,
    val title: String,
    val desc: String,
    val timeInMillis: Long,
    val frequency: String,
    val customDays: List<Int>,
    val isCompleted: Boolean = false,
    val isSnoozed: Boolean = false
)
