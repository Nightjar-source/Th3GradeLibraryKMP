package com.Nightjar.Th3GradeLibraryKMP

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object HeartbeatScheduler {
    fun scheduleHeartbeat(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HeartbeatReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2002, // Unique ID for Heartbeat
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule based on selected sync interval to bypass Doze efficiently without draining battery
        val syncInterval = com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine.appSettings.value.syncInterval
        val baseMinutes = when (syncInterval) {
            "15m" -> 15L
            "30m" -> 30L
            "1h"  -> 60L
            "6h"  -> 360L
            "12h" -> 720L
            "24h" -> 1440L
            else  -> 30L
        }
        
        // Add a random offset (2 to 9 minutes) acting as a "Dead Man's Switch".
        // The Alarm will only fire if the WorkManager fails to run and push it forward.
        val randomOffsetMinutes = (5..12).random()
        val intervalMinutes = baseMinutes + randomOffsetMinutes
        val intervalMillis = intervalMinutes * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis
 
        // Reverted to inexact alarms (setAndAllowWhileIdle) to strictly comply with Google Play policies,
        // since exact alarms are only intended for clock/calendar apps.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelHeartbeat(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HeartbeatReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2002, // Unique ID for Heartbeat
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
