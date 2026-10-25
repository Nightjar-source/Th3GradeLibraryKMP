package com.Nightjar.Th3GradeLibraryKMP.navigation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import com.Nightjar.Th3GradeLibraryKMP.ui.*

/**
 * Dedicated isolated destination composables (Navigation 3 pattern).
 * Isolates each screen's contract and dependencies to maintain clean separation of concerns.
 */

@Composable
fun RenderHomeDestination(
    isGridView: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToCategory: (String?) -> Unit,
    onNavigateToPdf: (BookItem) -> Unit,
    isDark: Boolean,
    onToggleViewMode: () -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onVoiceSearchTrigger: () -> Unit,
    bottomPadding: Dp,
    onScrollableStateChanged: (Boolean) -> Unit,
    scrollState: ScrollState
) {
    LibraryScreen(
        category = null,
        isGridView = isGridView,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onNavigateToCategory = onNavigateToCategory,
        onNavigateToPdf = onNavigateToPdf,
        isDark = isDark,
        onToggleViewMode = onToggleViewMode,
        isSearchOpen = isSearchOpen,
        onToggleSearch = onToggleSearch,
        onVoiceSearchTrigger = onVoiceSearchTrigger,
        bottomPadding = bottomPadding,
        onScrollableStateChanged = onScrollableStateChanged,
        scrollState = scrollState
    )
}

@Composable
fun RenderListDestination(
    category: String,
    isGridView: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToCategory: (String?) -> Unit,
    onNavigateToPdf: (BookItem) -> Unit,
    isDark: Boolean,
    onToggleViewMode: () -> Unit,
    isSearchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onVoiceSearchTrigger: () -> Unit,
    bottomPadding: Dp,
    onScrollableStateChanged: (Boolean) -> Unit,
    lazyGridState: LazyGridState
) {
    LibraryScreen(
        category = category,
        isGridView = isGridView,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onNavigateToCategory = onNavigateToCategory,
        onNavigateToPdf = onNavigateToPdf,
        isDark = isDark,
        onToggleViewMode = onToggleViewMode,
        isSearchOpen = isSearchOpen,
        onToggleSearch = onToggleSearch,
        onVoiceSearchTrigger = onVoiceSearchTrigger,
        bottomPadding = bottomPadding,
        onScrollableStateChanged = onScrollableStateChanged,
        lazyGridState = lazyGridState
    )
}

@Composable
fun RenderNewsDestination(
    onOpenSettings: () -> Unit,
    onItemClick: (String, String) -> Unit,
    isDark: Boolean,
    bottomPadding: Dp,
    onScrollableStateChanged: (Boolean) -> Unit,
    lazyGridState: LazyGridState
) {
    NewsScreen(
        onOpenSettings = onOpenSettings,
        onClick = onItemClick,
        isDark = isDark,
        bottomPadding = bottomPadding,
        onScrollableStateChanged = onScrollableStateChanged,
        lazyGridState = lazyGridState
    )
}

@Composable
fun RenderBookmarksDestination(
    savedItems: List<BookItem>,
    savedItemIds: Set<String>,
    isTransitionActive: Boolean,
    isReturningFromPdf: Boolean,
    activeTargetId: String?,
    isDark: Boolean,
    onNavigateToPdf: (BookItem, Int) -> Unit,
    bottomPadding: Dp,
    onScrollableStateChanged: (Boolean) -> Unit,
    lazyGridState: LazyGridState
) {
    val activeBookmarkKey = if ((isTransitionActive || isReturningFromPdf) && activeTargetId != null && activeTargetId.startsWith("bookmark_")) {
        val sub = activeTargetId.removePrefix("bookmark_")
        val bookId = sub.substringBeforeLast("_")
        val pageNum = sub.substringAfterLast("_")
        if (bookId.isNotEmpty() && pageNum.isNotEmpty()) "$bookId:$pageNum" else null
    } else null

    val effectiveSavedPages = remember(savedItemIds, activeBookmarkKey) {
        if (activeBookmarkKey != null && activeBookmarkKey !in savedItemIds) {
            savedItemIds + activeBookmarkKey
        } else {
            savedItemIds
        }
    }

    BookmarksScreen(
        savedItems = savedItems,
        savedPages = effectiveSavedPages.toList(),
        isDark = isDark,
        onNavigateToPdf = onNavigateToPdf,
        bottomPadding = bottomPadding,
        onScrollableStateChanged = onScrollableStateChanged,
        lazyGridState = lazyGridState
    )
}

@Composable
fun RenderShortcutsDestination(
    platformActionHandler: PlatformActionHandler,
    isDark: Boolean,
    bottomPadding: Dp,
    onScrollableStateChanged: (Boolean) -> Unit,
    lazyGridState: LazyGridState
) {
    ShortcutsScreen(
        platformActionHandler = platformActionHandler,
        isDark = isDark,
        bottomPadding = bottomPadding,
        onScrollableStateChanged = onScrollableStateChanged,
        lazyGridState = lazyGridState
    )
}

@Composable
fun RenderAboutDestination(
    onOpenUrl: (String) -> Unit,
    isDark: Boolean,
    bottomPadding: Dp,
    onScrollableStateChanged: (Boolean) -> Unit,
    scrollState: ScrollState
) {
    AboutScreen(
        onOpenUrl = onOpenUrl,
        isDark = isDark,
        bottomPadding = bottomPadding,
        onScrollableStateChanged = onScrollableStateChanged,
        scrollState = scrollState
    )
}

@Composable
fun RenderPdfDestination(
    item: BookItem,
    savedItemIds: Set<String>,
    onToggleBookmark: (Int) -> Unit,
    onClose: () -> Unit,
    initialPage: Int?,
    isDark: Boolean,
    isFromBookmarks: Boolean,
    sourcePage: String,
    isFromShortcut: Boolean = false
) {
    PdfViewerScreen(
        item = item,
        isSaved = { pageIdx -> "${item.id}:$pageIdx" in savedItemIds },
        onToggleSave = onToggleBookmark,
        onClose = onClose,
        initialPage = initialPage,
        isDark = isDark,
        isFromBookmarks = isFromBookmarks,
        sourcePage = sourcePage,
        isFromShortcut = isFromShortcut
    )
}
