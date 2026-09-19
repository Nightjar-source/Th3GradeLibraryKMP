package com.Nightjar.Th3GradeLibraryKMP.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * Official Google & JetBrains Compose Multiplatform Architecture (2026/2027)
 * Handles navigation backstack, modal states, search, and selected items cleanly decoupled from UI rendering.
 * Implements State Preservation against Process Death (February 2027 Memory Standards).
 */
class AppViewModel : ViewModel() {

    private val settings = AppDataStore

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

    private var hasInitializedLaunchState = false
    private var isProcessDeathState = false

    init {
        viewModelScope.launch {
            com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.isDataLoaded.collect { loaded ->
                if (loaded && isProcessDeathState && page == "home" && selectedItem == null) {
                    restoreState()
                }
            }
        }
    }

    /**
     * Initializes navigation state intelligently based on whether this is a cold launch or process death recovery:
     * - If [isProcessDeath] is true (app was killed by system in background while user was reading/navigating):
     *   Restores the exact previous page, selected book, and reading progress.
     * - If [isProcessDeath] is false (fresh cold launch by user after full close or swipe from recents):
     *   Starts cleanly at the "home" main screen, clearing any transient screen state while preserving all book progress in SyncEngine.
     */
    fun initLaunchState(isProcessDeath: Boolean, initialPage: String?, initialSearchQuery: String?) {
        if (hasInitializedLaunchState) return
        hasInitializedLaunchState = true
        isProcessDeathState = isProcessDeath

        if (isProcessDeath) {
            restoreState()
        } else {
            clearTransientNavState()
        }

        if (initialPage != null && initialPage != "home") {
            pageHistory.value = listOf("home", initialPage)
            page = initialPage
            persistState()
        }
        if (!initialSearchQuery.isNullOrEmpty()) {
            searchQuery = initialSearchQuery
        }
    }

    /**
     * Legacy helper that delegates to initLaunchState(isProcessDeath = false)
     */
    fun initDeepLink(initialPage: String?, initialSearchQuery: String?) {
        initLaunchState(isProcessDeath = false, initialPage = initialPage, initialSearchQuery = initialSearchQuery)
    }

    /**
     * Clears transient navigation state so fresh cold starts always land cleanly on the Home screen.
     */
    private fun clearTransientNavState() {
        try {
            settings.remove("nav_saved_page")
            settings.remove("nav_saved_book_id")
            settings.remove("nav_saved_pdf_page")
        } catch (_: Exception) {}
    }

    /**
     * Restores state against process death (Process Death Preservation)
     */
    private fun restoreState() {
        try {
            val savedBookId = settings.getString("nav_saved_book_id", "")
            val savedPdfPage = settings.getInt("nav_saved_pdf_page", -1)
            val savedPage = settings.getString("nav_saved_page", "home")

            if (savedBookId.isNotEmpty()) {
                val item = AllItems.getItemById(savedBookId)
                if (item != null) {
                    selectedItem = item
                    if (savedPdfPage > 0) {
                        pdfInitialPage = savedPdfPage
                    }
                }
            }

            if (savedPage.isNotEmpty() && savedPage != "home") {
                pageHistory.value = listOf("home", savedPage)
                page = savedPage
            }
        } catch (e: Exception) {
            // Graceful fallback to default home state
        }
    }

    /**
     * Persists active navigation state to Multiplatform Settings (instant small footprint <100 bytes)
     */
    private fun persistState() {
        try {
            settings.putString("nav_saved_page", page)
            if (selectedItem != null) {
                settings.putString("nav_saved_book_id", selectedItem!!.id)
            } else {
                settings.remove("nav_saved_book_id")
            }
            if (pdfInitialPage != null && pdfInitialPage!! > 0) {
                settings.putInt("nav_saved_pdf_page", pdfInitialPage!!)
            } else {
                settings.remove("nav_saved_pdf_page")
            }
        } catch (e: Exception) {
            // Ignore persistence errors
        }
    }

    /**
     * Updates current PDF page during reading
     */
    fun updatePdfPage(currentPage: Int) {
        if (currentPage > 0) {
            pdfInitialPage = currentPage
            try {
                settings.putInt("nav_saved_pdf_page", currentPage)
            } catch (e: Exception) { }
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
            persistState()
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
        persistState()
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
                persistState()
            }
        }
    }
}
