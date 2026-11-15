package com.Nightjar.Th3GradeLibraryKMP.core

/**
 * UI-Free Core for the bookmarks surface: grouping/aggregation logic for saved pages.
 * Pure functions — no Compose UI references — directly unit-testable.
 */
object BookmarksGrouper {

    /**
     * Combines the persisted set of "bookId:page" keys with the in-flight shared-transition target
     * (so the row currently animating out is kept visible during the cover-shrink animation),
     * then groups page numbers by book id.
     */
    fun group(
        savedPages: Collection<String>,
        activeTarget: String?,
        isTransitionActive: Boolean,
        isReturningFromPdf: Boolean
    ): Map<String, List<Int>> {
        val allPages = savedPages.toMutableSet()

        if ((isTransitionActive || isReturningFromPdf) && activeTarget != null && activeTarget.startsWith("bookmark_")) {
            val sub = activeTarget.removePrefix("bookmark_")
            val lastUnderscore = sub.lastIndexOf('_')
            if (lastUnderscore > 0 && lastUnderscore < sub.length - 1) {
                val bookId = sub.substring(0, lastUnderscore)
                val pageNum = sub.substring(lastUnderscore + 1)
                allPages.add("$bookId:$pageNum")
            }
        }

        return allPages.mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) {
                parts[0] to parts[1].toIntOrNull()
            } else null
        }
            .filter { it.second != null }
            .groupBy({ it.first }, { it.second!! })
    }
}
