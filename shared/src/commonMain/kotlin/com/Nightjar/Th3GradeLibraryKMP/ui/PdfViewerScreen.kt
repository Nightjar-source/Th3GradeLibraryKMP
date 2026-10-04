package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.runtime.Composable
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem

@Composable
expect fun PdfViewerScreen(
    item: BookItem,
    isSaved: (Int) -> Boolean,
    onToggleSave: (Int) -> Unit,
    onClose: () -> Unit,
    initialPage: Int? = null,
    isDark: Boolean = false,
    isFromBookmarks: Boolean = false
)
