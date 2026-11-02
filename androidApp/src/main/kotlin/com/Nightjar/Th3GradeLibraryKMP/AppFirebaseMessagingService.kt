package com.Nightjar.Th3GradeLibraryKMP

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import com.Nightjar.Th3GradeLibraryKMP.data.NewsItem
import com.Nightjar.Th3GradeLibraryKMP.network.SyncEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Official Firebase Cloud Messaging Service (FCM v1)
 * According to architectural guide: الوكيل/إشعارات_فايربيس_السحابية.md
 * 
 * - Zero Battery Drain: No polling or background WakeLocks needed on user devices.
 * - Silent Local Cache insertion into app_news_cache so items are available offline.
 * - Adheres strictly to user settings: notificationSound, grouping (news_group).
 * - Opens target news item directly in internal WebViewScreen or NewsScreen.
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_SOUND_ID = "news_channel_sound"
        const val CHANNEL_SILENT_ID = "news_channel_silent"
        const val GROUP_KEY = "news_group"
        const val NOTIFICATION_ID_BASE = 1000

        fun subscribeToNewsTopic() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("news")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        subscribeToNewsTopic()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "خبر دراسي جديد"
        val link = data["link"] ?: ""
        val imageUrl = data["image_url"] ?: data["imageUrl"] ?: ""
        val pubDate = data["pubDate"] ?: ""
        val content = data["content"] ?: remoteMessage.notification?.body ?: ""

        // Filter out temporary developer test items from persisting in student feed
        val isDevTest = title.contains("المطور") || title.contains("تجريبي") || link.startsWith("dev_")
        if (!isDevTest) {
            // 1. Silent Local Cache Update (Smart Watermark deduplication)
            saveToCache(title, link, imageUrl, pubDate, content)
        }

        // 2. Display System Notification respecting user's sync schedule to prevent 10-minute spam
        val syncInterval = SyncEngine.appSettings.value.syncInterval
        if (syncInterval != "on_open") {
            val intervalMs = when (syncInterval) {
                "15m" -> 15 * 60 * 1000L
                "30m" -> 30 * 60 * 1000L
                "1h"  -> 60 * 60 * 1000L
                "6h"  -> 6 * 60 * 60 * 1000L
                "12h" -> 12 * 60 * 60 * 1000L
                "24h" -> 24 * 60 * 60 * 1000L
                else  -> 30 * 60 * 1000L
            }
            val lastNotif = AppDataStore.getLong("last_fcm_news_alert_time", 0L)
            val now = System.currentTimeMillis()
            if (isDevTest || now - lastNotif >= intervalMs) {
                AppDataStore.putLong("last_fcm_news_alert_time", now)
                showNotification(title, content, link, imageUrl)
            }
        }
    }

    private fun saveToCache(title: String, link: String, imageUrl: String, pubDate: String, content: String) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val cachedJson = AppDataStore.getString("app_news_cache", "")
            val existingList: MutableList<NewsItem> = if (cachedJson.isNotEmpty()) {
                json.decodeFromString<List<NewsItem>>(cachedJson).toMutableList()
            } else {
                mutableListOf()
            }

            val id = if (link.isNotEmpty()) link else ("news_" + title.hashCode())
            val formattedDate = SyncEngine.formatNewsDate(pubDate)

            // Prevent duplicates (Smart Watermark)
            if (existingList.none { it.id == id || (it.title == title && it.pubDate == pubDate) }) {
                val newItem = NewsItem(
                    id = id,
                    title = title,
                    link = link,
                    contentSnippet = content,
                    pubDate = pubDate,
                    imageUrl = imageUrl.ifEmpty { null },
                    formattedDate = formattedDate
                )
                existingList.add(0, newItem)
                AppDataStore.putString("app_news_cache", json.encodeToString(existingList.take(60)))
                SyncEngine.reloadFromPersistence()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showNotification(title: String, content: String, link: String, imageUrl: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        val soundEnabled = SyncEngine.appSettings.value.notificationSound
        val channelId = if (soundEnabled) CHANNEL_SOUND_ID else CHANNEL_SILENT_ID

        // Intent to open directly in the app / WebView
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = Uri.parse(if (link.isNotEmpty()) "th3books://library?page=news&link=" + Uri.encode(link) else "th3books://library?page=news")
            putExtra("page", "news")
            if (link.isNotEmpty()) putExtra("link", link)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Download rich notification image/thumbnail with safety timeout (max 4s)
        var imageBitmap: android.graphics.Bitmap? = null
        if (imageUrl.isNotEmpty()) {
            try {
                val url = java.net.URL(imageUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.doInput = true
                connection.connect()
                val inputStream = connection.inputStream
                imageBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                // Silently fallback to standard text notification if image fails or timeouts
            }
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("أخبار الثالث متوسط")
            .setContentText(title)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY)

        if (imageBitmap != null) {
            notificationBuilder
                .setLargeIcon(imageBitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(imageBitmap)
                        .setSummaryText(if (content.isNotBlank()) content else title)
                        .bigLargeIcon(null as android.graphics.Bitmap?)
                )
        } else {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle().bigText(if (content.isNotBlank()) "$title\n$content" else title)
            )
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt() + NOTIFICATION_ID_BASE
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Sound Channel
            val soundChannel = NotificationChannel(
                CHANNEL_SOUND_ID,
                "أخبار الثالث متوسط (صوت)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات الأخبار مع صوت التنبيه المخصص"
                enableLights(true)
                enableVibration(true)
                val soundUri = Uri.parse("android.resource://$packageName/" + R.raw.custom_sound)
                setSound(soundUri, AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build())
            }

            // Channel 2: Silent Channel
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT_ID,
                "أخبار الثالث متوسط (صامت)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات الأخبار بدون صوت (هادئ)"
                setSound(null, null)
                enableVibration(false)
            }

            manager.createNotificationChannel(soundChannel)
            manager.createNotificationChannel(silentChannel)
        }
    }
}
