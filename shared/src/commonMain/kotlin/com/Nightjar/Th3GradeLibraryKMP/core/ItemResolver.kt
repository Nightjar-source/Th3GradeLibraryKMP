package com.Nightjar.Th3GradeLibraryKMP.core

import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem

/**
 * UI-Free Core: البحث عن الكتب والعناصر بالمعرف (id) — قرار خالص خالٍ من Compose.
 * يُختبر مباشرة بدون مُشغّل Android أو Compose.
 */
object ItemResolver {

    fun findById(id: String): BookItem? =
        AllItems.books.firstOrNull { it.id == id }
            ?: AllItems.notes.firstOrNull { it.id == id }
}
