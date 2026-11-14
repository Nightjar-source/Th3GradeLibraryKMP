package com.Nightjar.Th3GradeLibraryKMP

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Pure Cloud Push Architecture (Android 2027 & Google Play Standards):
 * 
 * Local alarms and RTC_WAKEUP are strictly disabled to ensure 0% battery drain,
 * zero process wakeups, and zero background wake lock pressure.
 * 
 * Background synchronization is driven 100% via Firebase Cloud Messaging (FCM Data Push)
 * and Cloudflare Edge triggers.
 */
object HeartbeatScheduler {
    fun scheduleHeartbeat(context: Context) {
        // Strictly no-op: Local AlarmManager wakeups are completely disabled in Cloud-Only architecture.
        // Background updates arrive silently via FCM v1 data messages.
    }

    fun cancelHeartbeat(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, HeartbeatReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2002,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (_: Exception) {}
    }
}
