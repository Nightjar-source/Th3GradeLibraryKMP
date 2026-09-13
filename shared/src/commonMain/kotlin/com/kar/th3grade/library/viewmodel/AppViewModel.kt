package com.Nightjar.gradeiraqi3library.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.Nightjar.gradeiraqi3library.data.BookItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * Official Google & JetBrains Compose Multiplatform Architecture (2026/2027)
 * Handles navigation backstack, modal states, search, and selected items cleanly decoupled from UI rendering.
 */
class AppViewModel : ViewModel() {

    // ===== Navigation State (State Hoisting) =====
    val pageHistory = mutableStateOf<List<String>>(listOf("home"))
    var page by mutableStateOf("home")
    var isNavigatingBack by mutableStateOf(false)

    // ===== Content Selection =====
    var selectedItem by mutableStateOf<BookItem?>(null)
    var pdfInitialPage by mutableStateOf<Int?>(null)

    // ===== Modals and Dialogs =====
    var isDrawerOpen by mutableStateOf(false)
    var isAppSettingsOpen by mutableStateOf(false)
    var isNewsSettingsOpen by mutableStateOf(false)
    var showClearCacheDialog by mutableStateOf(false)

    // ===== Search State =====
    var isSearchOpen by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    private var hasInitializedDeepLink = false

    /**
     * Initializes initial deep link or search query on app launch
     */
    fun initDeepLink(initialPage: String?, initialSearchQuery: String?) {
        if (hasInitializedDeepLink) return
        hasInitializedDeepLink = true

        if (initialPage != null && initialPage != "home") {
            pageHistory.value = listOf("home", initialPage)
            page = initialPage
        }
        if (!initialSearchQuery.isNullOrEmpty()) {
            searchQuery = initialSearchQuery
        }
    }

    /**
     * Pops top page from navigation backstack immutably (Immutable Safe Navigation)
     */
    fun popPage(): Boolean {
        isNavigatingBack = true
        val current = pageHistory.value
        if (current.size > 1) {
            val updated = current.dropLast(1)
            pageHistory.value = updated
            page = updated.last()
            return true
        }
        return false
    }

    /**
     * Navigates to a new page
     */
    fun navigateTo(newPage: String) {
        isNavigatingBack = false
        isAppSettingsOpen = false
        isNewsSettingsOpen = false
        if (newPage == "home") {
            pageHistory.value = listOf("home")
            page = "home"
        } else {
            val current = pageHistory.value
            if (newPage != current.lastOrNull()) {
                pageHistory.value = current.filter { it != newPage } + newPage
                page = newPage
            }
        }
    }

    private var clearSelectedItemJob: Job? = null

    /**
     * Safely selects an item for viewing and navigates to "pdf", canceling any pending clear job.
     */
    fun selectItem(item: BookItem, initialPage: Int? = null) {
        clearSelectedItemJob?.cancel()
        clearSelectedItemJob = null
        selectedItem = item
        pdfInitialPage = initialPage
        navigateTo("pdf")
    }

    /**
     * Clears selected item with 270ms delay for open cover animation, protected against race conditions.
     */
    fun clearSelectedItem() {
        clearSelectedItemJob?.cancel()
        popPage()
        clearSelectedItemJob = viewModelScope.launch {
            delay(270)
            if (page != "pdf") {
                selectedItem = null
                pdfInitialPage = null
            }
        }
    }
}
