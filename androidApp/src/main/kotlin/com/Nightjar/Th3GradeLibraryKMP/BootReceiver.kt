package com.Nightjar.Th3GradeLibraryKMP

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-schedule the Heartbeat
            HeartbeatScheduler.scheduleHeartbeat(context)
            // Re-schedule the WorkManager sync
            scheduleBackgroundSync(context)
        }
    }
}
