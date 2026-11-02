package com.Nightjar.Th3GradeLibraryKMP

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService

object NotificationHelper {
    const val CHANNEL_SILENT_ID = "news_sync_channel_silent_v2"
    const val CHANNEL_SOUND_ID = "news_sync_channel_sound_v2"
    const val CHANNEL_NAME = "أخبار المكتبة"
    const val SUMMARY_NOTIFICATION_ID = 0

    fun getCustomSoundUri(context: Context): Uri {
        return Uri.parse("android.resource://" + context.packageName + "/" + R.raw.custom_sound)
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService<NotificationManager>() ?: return
            
            // 1. Silent Channel
            if (nm.getNotificationChannel(CHANNEL_SILENT_ID) == null) {
                val silentChannel = NotificationChannel(
                    CHANNEL_SILENT_ID,
                    "$CHANNEL_NAME (صامتة)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات الأخبار الجديدة بدون تنبيه صوتي"
                    enableVibration(false)
                    setSound(null, null)
                }
                nm.createNotificationChannel(silentChannel)
            }
            
            // 2. Sound Channel
            if (nm.getNotificationChannel(CHANNEL_SOUND_ID) == null) {
                val soundUri = getCustomSoundUri(context)
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                val soundChannel = NotificationChannel(
                    CHANNEL_SOUND_ID,
                    "$CHANNEL_NAME (بالصوت)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "إشعارات الأخبار الجديدة مع تنبيه صوتي"
                    setSound(soundUri, audioAttrs)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 150, 250)
                }
                nm.createNotificationChannel(soundChannel)
            }
        }
    }

    fun getSyncNotification(context: Context): android.app.Notification {
        ensureNotificationChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_SILENT_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("جاري تحديث البيانات...")
            .setContentText("يتم الآن جلب آخر الأخبار في الخلفية.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun showNotifications(context: Context, titles: List<String>, count: Int, withSound: Boolean) {
        ensureNotificationChannels(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (titles.isEmpty()) return

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("page", "news")
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val targetChannelId = if (withSound) CHANNEL_SOUND_ID else CHANNEL_SILENT_ID
        val soundUri = if (withSound) getCustomSoundUri(context) else null

        // Unified Single Bundled Notification: lists items from newest to oldest
        val displayCount = if (count > 0) count else titles.size
        val mainTitle = if (displayCount == 1) titles.first() else "مكتبة الثالث متوسط: تم نشر $displayCount أخبار جديدة"
        val collapsedSummary = if (displayCount == 1) titles.first() else titles.take(3).joinToString(" • ")

        val inboxStyle = NotificationCompat.InboxStyle().apply {
            setBigContentTitle(mainTitle)
            setSummaryText("آخر التحديثات الوزارية")
            // Add lines from newest to oldest
            titles.take(6).forEach { addLine("• $it") }
        }

        val builder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(mainTitle)
            .setContentText(collapsedSummary)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .apply {
                if (withSound && soundUri != null) {
                    setSound(soundUri)
                } else {
                    setSilent(true)
                }
            }

        try {
            // Use SUMMARY_NOTIFICATION_ID so all updates overwrite cleanly into ONE notification
            NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
