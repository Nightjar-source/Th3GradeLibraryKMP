package com.Nightjar.Th3GradeLibraryKMP

import android.content.Context
import android.content.Intent
import android.app.Service
import android.os.IBinder

/**
 * Service that exposes specific library functions as tools for AI Agents (like Google Gemini).
 * This standardizes the app's agentic integration conceptually like an MCP server.
 */
class LibraryAppFunctionService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Searches the digital library for textbooks or study notes matching the query.
     */
    // @AppFunction
    fun searchLibrary(query: String): String {
        val context = applicationContext
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("AGENT_SEARCH_QUERY", query)
        }
        context.startActivity(intent)
        return "تم فتح المكتبة والبحث عن: $query"
    }

    /**
     * Opens the user's saved bookmarks and highlights.
     */
    // @AppFunction
    fun openBookmarks(): String {
        val context = applicationContext
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("AGENT_NAVIGATE_TO", "bookmarks")
        }
        context.startActivity(intent)
        return "تم فتح قسم المحفوظات بنجاح."
    }
}
