package com.Nightjar.Th3GradeLibraryKMP.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * UI-free holder for the pages a bookmarks accordion currently shows or animates out.
 *
 * Rules: tracking only grows (monotonic on accordion page changes), and a page is
 * prunable once it is neither an active shared-transition target nor still saved —
 * so the expand/shrink exit animation is never cut short by row removal.
 * No Compose imports besides snapshot state; the caller owns timing.
 */
class TrackedPagesTracker {

    /** Pages currently tracked for the accordion. */
    var pages: Set<Int> by mutableStateOf(emptySet())
        private set

    /** Adds every new page in [newPages] that is not tracked yet. */
    fun onPagesChange(newPages: List<Int>) {
        val current = pages.toMutableSet()
        var changed = false
        for (page in newPages) {
            if (current.add(page)) changed = true
        }
        if (changed) pages = current
    }

    /** Removes [page] (e.g. after its exit animation finished and it is not saved). */
    fun remove(page: Int) {
        pages = pages - page
    }
}
