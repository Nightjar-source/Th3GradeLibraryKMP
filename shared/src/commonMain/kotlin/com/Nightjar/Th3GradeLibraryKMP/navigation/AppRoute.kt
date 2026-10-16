package com.Nightjar.Th3GradeLibraryKMP.navigation

import kotlinx.serialization.Serializable

/**
 * Modern Jetpack Navigation 3 Route definitions (October 2026 / 2027 Standard).
 * Strongly-typed immutable sealed hierarchy representing each distinct navigation destination.
 */
@Serializable
sealed interface AppRoute {
    val routeKey: String

    @Serializable
    data object Home : AppRoute {
        override val routeKey: String = "home"
    }

    @Serializable
    data class List(val category: String? = null) : AppRoute {
        override val routeKey: String = "list"
    }

    @Serializable
    data object News : AppRoute {
        override val routeKey: String = "news"
    }

    @Serializable
    data object Bookmarks : AppRoute {
        override val routeKey: String = "bookmarks"
    }

    @Serializable
    data object Shortcuts : AppRoute {
        override val routeKey: String = "shortcuts"
    }

    @Serializable
    data object About : AppRoute {
        override val routeKey: String = "about"
    }

    @Serializable
    data class Pdf(
        val bookId: String,
        val initialPage: Int? = null,
        val fromBookmarks: Boolean = false,
        val sourcePage: String = "home"
    ) : AppRoute {
        override val routeKey: String = "pdf"
    }

    companion object {
        fun fromString(key: String, category: String? = null): AppRoute {
            return when (key) {
                "home" -> Home
                "list" -> List(category)
                "news" -> News
                "bookmarks" -> Bookmarks
                "shortcuts" -> Shortcuts
                "about" -> About
                else -> Home
            }
        }
    }
}
