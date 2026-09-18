package com.Nightjar.Th3GradeLibraryKMP.network

actual object PlatformClock {
    actual fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
