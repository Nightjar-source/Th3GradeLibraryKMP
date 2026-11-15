package com.Nightjar.Th3GradeLibraryKMP.core

import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem

/**
 * UI-Free Core: كل قرارات ترشيح الكتب (بحث/تصنيف) — دالة خالصة بلا أي مرجع لـ Compose UI.
 * تُختبر وحدها (unit-testable) والواجهة تستدعيها فقط.
 */
object LibraryFilter {
    /** Catalog for the given category (null = books shelf root, "books" = textbooks, anything else = notes). */
    fun catalogFor(category: String?): List<BookItem> =
        if (category == "books") AllItems.books else AllItems.notes

    /** Applies the search filter. Blank query returns the catalog untouched. */
    fun filter(catalog: List<BookItem>, query: String): List<BookItem> {
        if (query.isBlank()) return catalog
        return catalog.filter { it.title.contains(query, ignoreCase = true) }
    }
}
