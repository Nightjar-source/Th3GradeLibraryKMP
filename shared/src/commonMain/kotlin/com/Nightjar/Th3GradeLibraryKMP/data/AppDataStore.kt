package com.Nightjar.Th3GradeLibraryKMP.data

/**
 * Multiplatform persistent storage interface.
 * Android: Google Jetpack DataStore Preferences (/data/data/com.Nightjar.Th3GradeLibraryKMP/files/datastore/app_settings.preferences_pb)
 * Desktop: Persistent Local Storage (~/.Th3GradeLibraryKMP/app_settings.properties)
 */
expect object AppDataStore {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun remove(key: String)
}
