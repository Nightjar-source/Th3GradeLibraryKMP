package com.Nightjar.Th3GradeLibraryKMP.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.Nightjar.Th3GradeLibraryKMP.LibraryApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * Standard Jetpack DataStore file:
 * /data/data/com.Nightjar.Th3GradeLibraryKMP/files/datastore/app_settings.preferences_pb
 *
 * Includes SharedPreferencesMigration to seamlessly import existing bookmarks, last read pages,
 * and user preferences with zero data loss.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(context, "${context.packageName}_preferences"),
            SharedPreferencesMigration(context, "app_settings"),
            SharedPreferencesMigration(context, "AppPrefs")
        )
    }
)

actual object AppDataStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, Any>()
    private val context: Context get() = LibraryApplication.appContext

    init {
        try {
            // Preload all preferences into synchronous in-memory cache on app launch.
            // SharedPreferencesMigration is automatically performed on the first access.
            val initialPrefs = runBlocking(Dispatchers.IO) {
                context.dataStore.data.first()
            }
            initialPrefs.asMap().forEach { (key, value) ->
                cache[key.name] = value
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun getString(key: String, defaultValue: String): String {
        return (cache[key] as? String) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        cache[key] = value
        scope.launch {
            try {
                val prefKey = stringPreferencesKey(key)
                context.dataStore.edit { prefs ->
                    prefs[prefKey] = value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return (cache[key] as? Boolean) ?: defaultValue
    }

    actual fun putBoolean(key: String, value: Boolean) {
        cache[key] = value
        scope.launch {
            try {
                val prefKey = booleanPreferencesKey(key)
                context.dataStore.edit { prefs ->
                    prefs[prefKey] = value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return (cache[key] as? Int) ?: defaultValue
    }

    actual fun putInt(key: String, value: Int) {
        cache[key] = value
        scope.launch {
            try {
                val prefKey = intPreferencesKey(key)
                context.dataStore.edit { prefs ->
                    prefs[prefKey] = value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return (cache[key] as? Long) ?: defaultValue
    }

    actual fun putLong(key: String, value: Long) {
        cache[key] = value
        scope.launch {
            try {
                val prefKey = longPreferencesKey(key)
                context.dataStore.edit { prefs ->
                    prefs[prefKey] = value
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    actual fun remove(key: String) {
        cache.remove(key)
        scope.launch {
            try {
                val stringKey = stringPreferencesKey(key)
                val boolKey = booleanPreferencesKey(key)
                val intKey = intPreferencesKey(key)
                val longKey = longPreferencesKey(key)
                context.dataStore.edit { prefs ->
                    prefs.remove(stringKey)
                    prefs.remove(boolKey)
                    prefs.remove(intKey)
                    prefs.remove(longKey)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
