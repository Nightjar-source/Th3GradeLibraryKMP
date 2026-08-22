package com.Nightjar.gradeiraqi3library.network

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual object PlatformClock {
    actual fun currentTimeMillis(): Long {
        return jsDateNow().toLong()
    }
}
