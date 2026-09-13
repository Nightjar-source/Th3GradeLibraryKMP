package com.Nightjar.gradeiraqi3library.ui

import androidx.compose.runtime.Composable
import com.Nightjar.gradeiraqi3library.data.BookItem

@Composable
expect fun PdfViewerScreen(
    item: BookItem,
    isSaved: (Int) -> Boolean,
    onToggleSave: (Int) -> Unit,
    onClose: () -> Unit,
    initialPage: Int? = null,
    isDark: Boolean = false
)
