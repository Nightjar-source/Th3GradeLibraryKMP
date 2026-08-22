package com.Nightjar.gradeiraqi3library

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.os.PowerManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType

object WakeLockManager {
    var activeWakeLock: PowerManager.WakeLock? = null
}

class HeartbeatReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Acquire a WakeLock to ensure the CPU stays awake long enough for WorkManager to start
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Th3Grade:HeartbeatWakeLock")
        
        // 3 minutes max safety net, but will be released dynamically by WorkManager instantly
        wakeLock.acquire(3 * 60 * 1000L) 
        WakeLockManager.activeWakeLock = wakeLock

        // Enqueue a one-time work request to sync news immediately when the alarm fires
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val syncWork = OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
            .setConstraints(constraints)
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
            
        WorkManager.getInstance(context).enqueue(syncWork)
        
        // Reschedule next exact heartbeat
        HeartbeatScheduler.scheduleHeartbeat(context)
    }
}
