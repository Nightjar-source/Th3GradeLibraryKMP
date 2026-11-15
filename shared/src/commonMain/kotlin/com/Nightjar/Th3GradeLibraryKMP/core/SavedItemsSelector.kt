package com.Nightjar.Th3GradeLibraryKMP.core

import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem

/**
 * UI-Free Core: حفظ الصفحات المحيطة بالانتقال الحالي (شاملة الانتقال قيد التنفيذ)
 * ثم استخلاص الكتب المتاحة المرتبطة بتلك الصفحات.
 * دالة خالصة — لا تعتمد على Compose وتقرأ فقط من الوسائط.
 */
object SavedItemsSelector {

    fun select(
        savedKeys: Set<String>,
        activeTargetId: String?,
        isTransitionActive: Boolean,
        isReturningFromPdf: Boolean
    ): List<BookItem> {
        val savedBookIds = savedKeys.map { it.split(":")[0] }.toMutableSet()

        // Keep the in-flight book visible during its shared expand/shrink transition
        if ((isTransitionActive || isReturningFromPdf) && activeTargetId != null && activeTargetId.startsWith("bookmark_")) {
            val activeBookId = activeTargetId.substringAfter("bookmark_").substringBeforeLast("_")
            if (activeBookId.isNotEmpty()) savedBookIds.add(activeBookId)
        }

        return (AllItems.books + AllItems.notes).filter { it.id in savedBookIds }
    }
}
