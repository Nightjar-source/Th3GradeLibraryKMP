package com.Nightjar.Th3GradeLibraryKMP.core

import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine

/**
 * UI-Free Core: عمليات المحفوظات (toggle + رسالة المستخدم).
 * الواجهة تقرأ النتيجة وتعرض التوست؛ لا تقرر هي منطق الحفظ/الحذف.
 */
object BookmarkActions {

    data class BookmarkToggleResult(val isSaved: Boolean, val toastMessage: String)

    fun toggle(item: BookItem, pageIndex: Int): BookmarkToggleResult {
        val key = "${item.id}:$pageIndex"
        val isNowSaved = SyncEngine.toggleBookmark(key)
        val msg = if (isNowSaved) "تم حفظ الصفحة ${pageIndex + 1} في المحفوظات"
                  else "تم حذف الصفحة ${pageIndex + 1} من المحفوظات"
        return BookmarkToggleResult(isNowSaved, msg)
    }
}
