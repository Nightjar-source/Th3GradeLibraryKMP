package com.Nightjar.Th3GradeLibraryKMP.data

/**
 * Web / wasmJs implementation of PlatformMemoryManager (February 2027 Standards).
 */
actual object PlatformMemoryManager {
    actual fun trimMemory() {
        // No-op on Web
    }

    actual fun clearDeepCache() {
        // No-op on Web
    }
}
