package com.Nightjar.Th3GradeLibraryKMP.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.Nightjar.Th3GradeLibraryKMP.data.NewsItem
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine

/**
 * UI-Free Core for the news feed: كل قرارات وقواعد العمل (قراءة/حذف/تراجع/استعادة)
 * تعيش هنا — لا يوجد أي منطق عمل داخل Composable. الواجهة تقرأ فقط.
 */
class NewsFeedState {

    // Item currently in the undo slot (null when the banner is hidden)
    var pendingUndoItem by mutableStateOf<NewsItem?>(null)
        private set
    var pendingUndoIndex by mutableStateOf(-1)
        private set
    var showUndoBanner by mutableStateOf(false)
        private set

    fun isRead(item: NewsItem, readIds: Set<String>): Boolean =
        readIds.contains(item.id) || (item.link.isNotEmpty() && readIds.contains(item.link))

    /** Marks the item read (persists via SyncEngine). */
    fun markRead(item: NewsItem) {
        SyncEngine.markNewsAsRead(item.id)
        if (item.link.isNotEmpty()) SyncEngine.markNewsAsRead(item.link)
    }

    /** Deletes the item at its list position and opens the undo banner. No-op if absent. */
    fun deleteWithUndo(item: NewsItem, list: List<NewsItem>) {
        val index = list.indexOf(item)
        if (index < 0) return
        pendingUndoItem = item
        pendingUndoIndex = index
        SyncEngine.deleteNewsItem(item.id)
        showUndoBanner = true
    }

    /** Restores the last deleted item at its original position. Returns false if nothing to restore. */
    fun undoRestore(): Boolean {
        val item = pendingUndoItem ?: return false
        if (pendingUndoIndex >= 0) SyncEngine.restoreNewsItem(item, pendingUndoIndex)
        dismiss()
        return true
    }

    /** Hides the undo banner without restoring the item. */
    fun dismiss() {
        showUndoBanner = false
        pendingUndoItem = null
        pendingUndoIndex = -1
    }
}
