package com.Nightjar.Th3GradeLibraryKMP.data

interface PlatformActionHandler {
    fun addHomeScreenShortcut(item: BookItem)
    fun showToast(message: String)
    fun openUrl(url: String)
    fun openCustomTab(url: String, colorHex: String) {}
    fun playNotificationSound()
    fun startVoiceSearch(onResult: (String) -> Unit, onEnd: () -> Unit)
    /** Called whenever syncInterval setting changes so WorkManager can be rescheduled. */
    fun rescheduleBackgroundSync() {}
    fun areNotificationsEnabled(): Boolean { return true }
    fun openNotificationSettings() {}
    /** Shows a system notification when news is fetched in the foreground */
    fun showNewsNotification(titles: List<String>, count: Int) {}
    fun isBatteryOptimizationIgnored(): Boolean { return true }
    fun requestBatteryOptimizationExemption() {}
    fun canScheduleExactAlarms(): Boolean { return true }
    fun requestExactAlarmPermission() {}
    fun requestNotificationPermission() {}
    fun isAutoRevokeWhitelisted(): Boolean { return true }
    fun requestAutoRevokeExemption() {}
    /** Graceful degradation: checks if device is low RAM (<3GB) or running old OS (<= Android 9 API 28) */
    fun isLowEndDevice(): Boolean { return false }
    /** Clears all disk cache including temporary PDF files and image cache */
    fun clearDiskCache() {}
    /** Dynamically updates system status bar and 3-button navigation bar icon appearance in real time */
    fun updateSystemBars(isLightStatusBars: Boolean, isLightNavigationBars: Boolean) {}
}
