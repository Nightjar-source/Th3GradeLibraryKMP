package com.Nightjar.Th3GradeLibraryKMP

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * Universal Notification Helper for Android 7.0 (API 24) to Android 17 (API 36+)
 * 
 * - Full Legacy Support: Android 7.0/7.1 use direct builder sound/vibration.
 * - Channels Architecture: Android 8.0+ uses NotificationChannel with custom sound.
 * - Runtime Permissions: Android 13+ guards with POST_NOTIFICATIONS check.
 * - Strict Memory Limits: Android 15/16/17 Binder buffer protection with micro-thumbnails (48x48px, ~3.5KB).
 * - Bundling: Aggregates multiple news items into a single InboxStyle notification.
 */
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

    fun loadMicroThumbnail(imageUrl: String): Bitmap? {
        if (imageUrl.isBlank()) return null
        return try {
            val url = java.net.URL(imageUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 2500
            connection.readTimeout = 2500
            connection.doInput = true
            connection.connect()
            val bytes = connection.inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) return null

            val boundsOptions = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val reqDim = 48
            var inSample = 1
            if (boundsOptions.outHeight > reqDim || boundsOptions.outWidth > reqDim) {
                val halfH = boundsOptions.outHeight / 2
                val halfW = boundsOptions.outWidth / 2
                while ((halfH / inSample) >= reqDim && (halfW / inSample) >= reqDim) {
                    inSample *= 2
                }
            }

            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            val minEdge = decoded.width.coerceAtMost(decoded.height)
            val cropX = (decoded.width - minEdge) / 2
            val cropY = (decoded.height - minEdge) / 2
            val squared = Bitmap.createBitmap(decoded, cropX, cropY, minEdge, minEdge)
            Bitmap.createScaledBitmap(squared, reqDim, reqDim, true)
        } catch (_: Exception) {
            null
        }
    }

    fun getSyncNotification(context: Context): android.app.Notification {
        ensureNotificationChannels(context)
        return NotificationCompat.Builder(context, CHANNEL_SILENT_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(android.graphics.Color.parseColor("#2563EB"))
            .setContentTitle("جاري تحديث البيانات...")
            .setContentText("يتم الآن جلب آخر الأخبار في الخلفية.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun showNotifications(
        context: Context,
        titles: List<String>,
        count: Int,
        withSound: Boolean,
        microThumbnail: Bitmap? = null
    ) {
        ensureNotificationChannels(context)

        // Strict Android 7 to 17 permission validation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        } else {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        }

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

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(mainTitle)
            .setSummaryText("آخر التحديثات الوزارية")
        titles.take(6).forEach { title ->
            inboxStyle.addLine("• $title")
        }

        val builder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(android.graphics.Color.parseColor("#2563EB"))
            .setContentTitle(mainTitle)
            .setContentText(collapsedSummary)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)

        // Ultra-compact micro-thumbnail (text line size ~48x48px)
        var finalThumb = microThumbnail
        if (finalThumb == null) {
            try {
                val cachedJson = com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore.getString("app_news_cache", "")
                if (cachedJson.isNotEmpty()) {
                    val list = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        .decodeFromString<List<com.Nightjar.Th3GradeLibraryKMP.data.NewsItem>>(cachedJson)
                    val imgUrl = list.firstOrNull { it.imageUrl?.isNotBlank() == true }?.imageUrl
                    if (!imgUrl.isNullOrBlank()) {
                        finalThumb = loadMicroThumbnail(imgUrl)
                    }
                }
            } catch (_: Exception) {}
        }
        if (finalThumb != null) {
            builder.setLargeIcon(finalThumb)
        }

        // Sound & Vibration backwards compatibility:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ uses channel sound configuration
            if (!withSound) {
                builder.setSilent(true)
            }
        } else {
            // Android 7.0 & 7.1 legacy fallback
            if (withSound && soundUri != null) {
                builder.setSound(soundUri)
                builder.setVibrate(longArrayOf(0, 250, 150, 250))
            } else {
                builder.setSilent(true)
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
