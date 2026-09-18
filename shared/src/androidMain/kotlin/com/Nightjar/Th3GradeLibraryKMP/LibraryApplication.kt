package com.Nightjar.Th3GradeLibraryKMP

import android.app.Application
import android.content.Context

open class LibraryApplication : Application() {
    companion object {
        lateinit var appContext: Context
            internal set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }
}
