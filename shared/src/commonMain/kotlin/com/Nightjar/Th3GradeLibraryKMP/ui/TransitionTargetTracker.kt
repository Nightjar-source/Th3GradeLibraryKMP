package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared element transition tracker.
 */
object TransitionTargetTracker {
    /**
     * Currently active target item ID participating in an expand/shrink transition.
     */
    var activeTargetId by mutableStateOf<String?>(null)

    /**
     * Whether a shared transition is currently in flight.
     */
    var isTransitionActive by mutableStateOf(false)

    /**
     * Resets tracking state upon transition completion.
     */
    fun reset() {
        activeTargetId = null
        isTransitionActive = false
    }
}
