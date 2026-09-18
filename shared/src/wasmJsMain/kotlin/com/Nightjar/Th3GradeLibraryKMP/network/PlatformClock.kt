package com.Nightjar.Th3GradeLibraryKMP.network

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual object PlatformClock {
    actual fun currentTimeMillis(): Long {
        return jsDateNow().toLong()
    }
}
