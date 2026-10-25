package com.Nightjar.Th3GradeLibraryKMP.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem

/**
 * Loads the rendered first page of the PDF file (or user's saved reading page)
 * as a fallback thumbnail when a static cover image is missing.
 *
 * @param item The target BookItem containing pdfPath and id.
 * @param targetPage The specific page index to render (defaults to page 0, or user's saved last-read page).
 * @return ImageBitmap if rendered successfully, null otherwise.
 */
@Composable
expect fun rememberPdfPageThumbnail(
    item: BookItem,
    targetPage: Int = 0
): ImageBitmap?
