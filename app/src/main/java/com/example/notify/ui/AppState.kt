package com.example.notify.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Global observable state for cross-component communication.
 */
object AppState {
    /** The ID of the reminder currently being edited, or null for a new reminder. */
    var editingId: Int? by mutableStateOf(null)

    /** The ID of the reminder currently being viewed in the detail screen. */
    var viewingId: Int? by mutableStateOf(null)

    /** Incremented to force UI recomposition after data changes. */
    var forceRefresh: Int by mutableStateOf(0)

    /** Incremented to force theme recomposition after theme changes. */
    var themeRefresh: Int by mutableStateOf(0)
}
