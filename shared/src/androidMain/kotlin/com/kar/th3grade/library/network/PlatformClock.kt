package com.Nightjar.gradeiraqi3library.network

actual object PlatformClock {
    actual fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
