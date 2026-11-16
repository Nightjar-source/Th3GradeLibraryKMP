package com.Nightjar.Th3GradeLibraryKMP.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavBackStack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.AllItems
import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import com.Nightjar.Th3GradeLibraryKMP.navigation.AppRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * Official Jetpack Navigation 3 architecture (Compose Multiplatform, 2026/2027 Standard).
 * Source of truth for navigation: [backStack] — immutable immutable [NavBackStack].
 * [page] and [pageHistory] are derived compatibility getters used by top bar/bottom bar wiring.
 */
class AppViewModel : ViewModel() {

    private val settings = AppDataStore

    // ===== Navigation 3 Official BackStack =====
    val backStack: NavBackStack<AppRoute> = NavBackStack(AppRoute.Home)

    // Derived read-only façade for legacy string-keyed wiring (top bar, bottom bar, side rail)
    val page: String
        get() = when (val top = backStack.lastOrNull()) {
            is AppRoute.Home -> "home"
            is AppRoute.List -> "list"
            is AppRoute.News -> "news"
            is AppRoute.Bookmarks -> "bookmarks"
            is AppRoute.Shortcuts -> "shortcuts"
            is AppRoute.About -> "about"
            is AppRoute.Pdf -> "pdf"
            else -> "home"
        }

    val pageHistory: List<String>
        get() = backStack.map { it.routeKey }

    var isNavigatingBack by mutableStateOf(false)
    var isReturningFromPdf by mutableStateOf(false)
    var screenNavCounter by mutableStateOf(0)

    // ===== Content Selection =====
    var selectedItem by mutableStateOf<BookItem?>(null)
    var pdfInitialPage by mutableStateOf<Int?>(null)
    var isFromBookmarks by mutableStateOf(false)
    var pdfSourcePage by mutableStateOf("home")

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
                if (loaded && page == "home" && selectedItem == null) {
                    restoreState()
                }
            }
        }
    }

    private fun toRoute(page: String, category: String? = null): AppRoute =
        when (page) {
            "home" -> AppRoute.Home
            "list" -> AppRoute.List(category)
            "news" -> AppRoute.News
            "bookmarks" -> AppRoute.Bookmarks
            "shortcuts" -> AppRoute.Shortcuts
            "about" -> AppRoute.About
            "pdf" -> AppRoute.Pdf(selectedItem?.id ?: "", pdfInitialPage)
            else -> AppRoute.Home
        }

    private fun toRouteList(keys: List<String>, category: String? = null): List<AppRoute> =
        keys.map { toRoute(it, category) }

    fun initLaunchState(
        isProcessDeath: Boolean,
        initialPage: String?,
        initialSearchQuery: String?,
        initialBookId: String? = null,
        initialIsNote: Boolean = false
    ) {
        if (hasInitializedLaunchState) {
            if (initialBookId != null) {
                val target = AllItems.books.firstOrNull { it.id == initialBookId }
                    ?: AllItems.notes.firstOrNull { it.id == initialBookId }
                if (target != null) {
                    com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId = "card_${target.id}"
                    com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.isTransitionActive = true
                    selectedItem = target
                    pdfInitialPage = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.lastReadPages.value[target.id]
                    com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.saveSelectedCategory(if (target.isNote) "notes" else "books")
                    pdfSourcePage = "list"
                    backStack.clear()
                    backStack.add(AppRoute.Home)
                    backStack.add(AppRoute.List())
                    backStack.add(AppRoute.Pdf(target.id, pdfInitialPage))
                    persistState()
                }
            }
            return
        }
        hasInitializedLaunchState = true
        isProcessDeathState = isProcessDeath

        restoreState()

        if (initialBookId != null) {
            val target = AllItems.books.firstOrNull { it.id == initialBookId }
                ?: AllItems.notes.firstOrNull { it.id == initialBookId }
            if (target != null) {
                com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.activeTargetId = "card_${target.id}"
                com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.isTransitionActive = true
                selectedItem = target
                pdfInitialPage = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.lastReadPages.value[target.id]
                com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.saveSelectedCategory(if (target.isNote) "notes" else "books")
                pdfSourcePage = "list"
                backStack.clear()
                backStack.add(AppRoute.Home)
                backStack.add(AppRoute.List())
                backStack.add(AppRoute.Pdf(target.id, pdfInitialPage))
                persistState()
            }
        } else if (initialPage != null && initialPage != "home") {
            backStack.clear()
            backStack.add(AppRoute.Home)
            backStack.add(toRoute(initialPage))
            persistState()
        }
        if (!initialSearchQuery.isNullOrEmpty()) {
            searchQuery = initialSearchQuery
        }
    }

    fun initDeepLink(initialPage: String?, initialSearchQuery: String?) {
        initLaunchState(isProcessDeath = false, initialPage = initialPage, initialSearchQuery = initialSearchQuery)
    }

    private fun clearTransientNavState() {
        try {
            settings.remove("nav_saved_page")
            settings.remove("nav_saved_book_id")
            settings.remove("nav_saved_pdf_page")
        } catch (_: Exception) {}
    }

    private fun restoreState() {
        try {
            val savedBookId = settings.getString("nav_saved_book_id", "")
            val savedPdfPage = settings.getInt("nav_saved_pdf_page", -1)
            val savedPage = settings.getString("nav_saved_page", "home")

            if (savedBookId.isNotEmpty()) {
                val item = AllItems.getItemById(savedBookId)
                if (item != null) {
                    val lastSaved = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.lastReadPages.value[item.id] ?: 0
                    val targetPage = if (savedPdfPage >= 0) savedPdfPage else lastSaved
                    selectedItem = item
                    pdfInitialPage = targetPage
                    pdfSourcePage = "list"
                    backStack.clear()
                    backStack.add(AppRoute.Home)
                    backStack.add(AppRoute.List())
                    backStack.add(AppRoute.Pdf(item.id, targetPage))
                    return
                }
            }

            if (savedPage.isNotEmpty() && savedPage != "home") {
                backStack.clear()
                backStack.add(AppRoute.Home)
                backStack.add(toRoute(savedPage))
            }
        } catch (e: Exception) {
            // Graceful fallback to default home state
        }
    }

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

    fun updatePdfPage(currentPage: Int) {
        if (currentPage > 0) {
            pdfInitialPage = currentPage
            try {
                settings.putInt("nav_saved_pdf_page", currentPage)
            } catch (e: Exception) { }
        }
    }

    /**
     * Pops the top page from the Navigation 3 backstack (Immutable Safe Navigation).
     */
    fun popPage(): Boolean {
        isNavigatingBack = true
        val wasOnPdf = page == "pdf"
        if (wasOnPdf) {
            isReturningFromPdf = true
        }
        val removed = backStack.removeLastOrNull()
        if (removed != null && backStack.isNotEmpty()) {
            if (!wasOnPdf) {
                screenNavCounter++
            }
            persistState()
            viewModelScope.launch {
                delay(450)
                isNavigatingBack = false
                if (wasOnPdf) {
                    isReturningFromPdf = false
                }
            }
            return true
        }
        viewModelScope.launch {
            delay(450)
            isNavigatingBack = false
            if (wasOnPdf) {
                isReturningFromPdf = false
            }
        }
        return false
    }

    /**
     * Navigates to a new page (Navigation 3 push onto the backstack).
     */
    fun navigateTo(newPage: String) {
        isNavigatingBack = false
        if (newPage != "home" && newPage != "list" && newPage != "bookmarks") {
            isReturningFromPdf = false
        }
        isAppSettingsOpen = false
        isNewsSettingsOpen = false
        if (newPage == "home") {
            if (page != "home") screenNavCounter++
            backStack.clear()
            backStack.add(AppRoute.Home)
        } else {
            val currentTop = page
            if (newPage != currentTop) {
                if (newPage != "pdf") {
                    screenNavCounter++
                }
                // Remove any existing occurrence to keep a flat single-table backstack shape
                val existing = backStack.indexOfFirst { it.routeKey == newPage }
                if (existing >= 0) {
                    backStack.removeAt(existing)
                }
                backStack.add(toRoute(newPage))
            }
        }
        persistState()
    }

    private var clearSelectedItemJob: Job? = null

    /**
     * Safely selects an item for viewing and navigates to "pdf", canceling any pending clear job.
     */
    fun selectItem(item: BookItem, initialPage: Int? = null, fromBookmarks: Boolean = false) {
        clearSelectedItemJob?.cancel()
        clearSelectedItemJob = null
        pdfSourcePage = page
        selectedItem = item
        pdfInitialPage = initialPage
        isFromBookmarks = fromBookmarks
        navigateTo("pdf")
    }

    /**
     * Clears selected item with 450ms delay for close cover spring animation, protected against race conditions.
     */
    fun clearSelectedItem() {
        clearSelectedItemJob?.cancel()
        popPage()
        clearSelectedItemJob = viewModelScope.launch {
            delay(450)
            if (page != "pdf") {
                selectedItem = null
                pdfInitialPage = null
                isFromBookmarks = false
                pdfSourcePage = "home"
                com.Nightjar.Th3GradeLibraryKMP.ui.TransitionTargetTracker.reset()
                try {
                    settings.remove("nav_saved_book_id")
                    settings.remove("nav_saved_pdf_page")
                } catch (_: Exception) {}
                persistState()
            }
            isNavigatingBack = false
        }
    }
}
