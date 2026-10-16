package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Real-time 120Hz Coordinate Tracker for Compose Shared Element Transitions.
 * Tracks target item coordinates and live overscroll displacements dynamically,
 * eliminating the 300ms cover freeze during rapid scrolling and rubber-band drag.
 */
object TransitionTargetTracker {
    /**
     * Currently active target item ID participating in an expand/shrink transition.
     */
    var activeTargetId by mutableStateOf<String?>(null)

    /**
     * Real-time window coordinates of the active target card.
     */
    var activeTargetBounds by mutableStateOf<Rect?>(null)

    /**
     * Current rubber-band overscroll displacement in pixels.
     */
    var currentOverscrollY by mutableStateOf(0f)

    /**
     * Whether a shared transition is currently in flight.
     */
    var isTransitionActive by mutableStateOf(false)

    /**
     * Set of currently visible item IDs in the library grid to smartly detect warm starts.
     */
    var visibleItemIds by mutableStateOf<Set<String>>(emptySet())

    /**
     * Updates the reported real-time bounds of a target item.
     */
    fun updateTargetBounds(id: String, bounds: Rect) {
        if (id == activeTargetId) {
            activeTargetBounds = bounds
        }
    }

    /**
     * Resets tracking state upon transition completion.
     */
    fun reset() {
        activeTargetId = null
        activeTargetBounds = null
        currentOverscrollY = 0f
        isTransitionActive = false
    }
}
