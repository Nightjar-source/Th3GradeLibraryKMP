package com.Nightjar.Th3GradeLibraryKMP.core

/**
 * UI-Free Core: قرار "ماذا يحدث عند الضغط على زر الرجوع" —
 * دالة خالصة تتلقى الحالة الحالية وتُرجع إجراءً. الواجهة تنفذه فقط.
 */
object BackNavigator {

    enum class BackAction {
        CLOSE_CACHE_DIALOG,
        CLOSE_DRAWER,
        CLOSE_NEWS_SETTINGS,
        CLOSE_APP_SETTINGS,
        CLOSE_PDF,
        NAVIGATE_HOME,
        POP_PAGE,
        STAY
    }

    /**
     * Decides what the global back action should do.
     * Priority order matches the unified top bar's back handler — testable pure function.
     */
    fun decide(
        showClearCacheDialog: Boolean,
        isDrawerOpen: Boolean,
        isNewsSettingsOpen: Boolean,
        isAppSettingsOpen: Boolean,
        hasSelectedItem: Boolean,
        page: String,
        canPopPage: Boolean
    ): BackAction = when {
        showClearCacheDialog -> BackAction.CLOSE_CACHE_DIALOG
        isDrawerOpen -> BackAction.CLOSE_DRAWER
        isNewsSettingsOpen -> BackAction.CLOSE_NEWS_SETTINGS
        isAppSettingsOpen -> BackAction.CLOSE_APP_SETTINGS
        hasSelectedItem -> BackAction.CLOSE_PDF
        page == "list" -> BackAction.NAVIGATE_HOME
        canPopPage -> BackAction.POP_PAGE
        else -> BackAction.NAVIGATE_HOME
    }
}
