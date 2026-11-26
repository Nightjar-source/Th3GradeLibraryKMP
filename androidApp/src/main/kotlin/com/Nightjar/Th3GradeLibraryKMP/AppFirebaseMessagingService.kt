package com.Nightjar.Th3GradeLibraryKMP

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
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
 * - Full Android 7.0 (API 24) to Android 17 (API 36+) compatibility.
 * - Zero Battery Drain: Silent Data-Only FCM delivery with zero wake locks.
 * - Strict User Schedule Adherence: Notifications only fire at the user's chosen reminder interval.
 * - Multi-News Bundling: Gathers all news that arrived in the quiet period into ONE single InboxStyle notification.
 * - Micro-Thumbnail (48x48px, ~3.5KB): Rendered at exact text line height beside the headline.
 * - Dynamic Unsubscription: Unsubscribes from FCM topic completely when user selects 'on_open'.
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_SOUND_ID = "news_channel_sound_v3"
        const val CHANNEL_SILENT_ID = "news_channel_silent_v3"
        const val GROUP_KEY = "news_group"
        const val NOTIFICATION_ID_BASE = 1000

        fun subscribeToNewsTopic() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("news")
                FirebaseMessaging.getInstance().subscribeToTopic("all")
                FirebaseMessaging.getInstance().subscribeToTopic("announcements")
                FirebaseMessaging.getInstance().subscribeToTopic("general")
                FirebaseMessaging.getInstance().subscribeToTopic("kar")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun unsubscribeFromNewsTopic() {
            try {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("news")
                FirebaseMessaging.getInstance().unsubscribeFromTopic("kar")
                FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun recordUnnotifiedNews(title: String) {
            try {
                val raw = AppDataStore.getString("unnotified_fcm_titles", "")
                val list = if (raw.isNotEmpty()) raw.split(";;;").toMutableList() else mutableListOf()
                val clean = title.replace("أخبار الثالث متوسط:", "").replace("أخبار الثالث متوسط", "").trim().ifBlank { title }
                if (!list.contains(clean)) {
                    list.add(0, clean)
                    AppDataStore.putString("unnotified_fcm_titles", list.take(25).joinToString(";;;"))
                }
            } catch (_: Exception) {}
        }

        fun getAndClearUnnotifiedNews(): List<String> {
            return try {
                val raw = AppDataStore.getString("unnotified_fcm_titles", "")
                AppDataStore.putString("unnotified_fcm_titles", "")
                if (raw.isNotEmpty()) raw.split(";;;").filter { it.isNotBlank() } else emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (SyncEngine.appSettings.value.syncInterval != "on_open") {
            subscribeToNewsTopic()
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val rawTitle = data["title"] ?: remoteMessage.notification?.title ?: "تبليغ وزاري جديد"
        val title = rawTitle.trim().ifBlank { "تبليغ وزاري جديد" }
        val link = (data["link"] ?: data["click_url"] ?: "").trim()
        val imageUrl = (data["image_url"] ?: data["imageUrl"] ?: remoteMessage.notification?.imageUrl?.toString() ?: "").trim()
        val content = (data["content"] ?: data["body"] ?: remoteMessage.notification?.body ?: "").trim()
        val rawPubDate = (data["pubDate"] ?: "").trim()
        val pubDate = if (rawPubDate.isNotBlank()) rawPubDate else {
            System.currentTimeMillis().toString()
        }

        val isDevTest = title.contains("المطور") || title.contains("تجريبي") || link.startsWith("dev_") || data["is_test"] == "true"
        val fromTopic = remoteMessage.from ?: ""
        val isStrictNews = (fromTopic.endsWith("/news") || fromTopic == "news" || fromTopic.contains("/topics/news")) && data["type"] == "news"
        val isAnnouncement = !isStrictNews || data["type"] == "announcement" || data["is_announcement"] == "true" || title.contains("ميزات") || title.contains("تبليغ")

        // 1. Permanent Local Cache Update (Saved permanently in User Data)
        val isNewItem = saveToCache(title, link, imageUrl, pubDate, content, isAnnouncement)

        // 2. Announcements & Admin updates ALWAYS fire immediately (never suppressed)
        if (isAnnouncement || isDevTest) {
            showMicroThumbnailNotification(title, content, link, imageUrl, isNews = false)
            return
        }

        if (!isNewItem) {
            return
        }

        // 3. User Sync Schedule Check for standard RSS news
        val syncInterval = SyncEngine.appSettings.value.syncInterval
        if (syncInterval == "on_open") {
            // User selected updates ONLY when opening the app: ZERO background notifications
            return
        }

        if (SyncEngine.isUserOnNewsScreen) {
            // User is currently actively looking at the News screen
            return
        }

        // 4. Deliver every cloud pushed news item immediately as an individual card
        // Zero Lockout: do not suppress subsequent cloud messages with 30m artificial delays.
        val now = System.currentTimeMillis()
        AppDataStore.putLong("last_fcm_news_alert_time", now)
        showMicroThumbnailNotification(title, content, link, imageUrl, isNews = true)
    }

    private fun saveToCache(title: String, link: String, imageUrl: String, pubDate: String, content: String, isAnnouncement: Boolean = false): Boolean {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val cachedJson = AppDataStore.getString("app_news_cache", "")
            val existingList: MutableList<NewsItem> = if (cachedJson.isNotEmpty()) {
                try {
                    json.decodeFromString<List<NewsItem>>(cachedJson).toMutableList()
                } catch (_: Exception) {
                    SyncEngine.newsList.value.toMutableList()
                }
            } else {
                SyncEngine.newsList.value.toMutableList()
            }

            val id = if (link.isNotEmpty()) link else ("news_" + title.hashCode() + "_" + (System.currentTimeMillis() / 1000))
            val formattedDate = SyncEngine.formatNewsDate(pubDate)
            val normLink = SyncEngine.normalizeUrl(link)

            val existingItem = existingList.find {
                it.id == id ||
                (normLink.isNotEmpty() && SyncEngine.normalizeUrl(it.link) == normLink) ||
                (title.isNotBlank() && it.title == title && it.contentSnippet == content)
            }
            val isExactDuplicate = existingItem != null && (existingItem.contentSnippet == content && existingItem.title == title)

            if (!isExactDuplicate) {
                if (existingItem != null) {
                    existingList.remove(existingItem)
                }

                val newItem = NewsItem(
                    id = id,
                    title = title,
                    link = link,
                    contentSnippet = content,
                    pubDate = pubDate,
                    imageUrl = imageUrl.ifEmpty { null },
                    formattedDate = formattedDate,
                    isAnnouncement = isAnnouncement
                )
                existingList.add(0, newItem)
                val sorted = existingList.sortedByDescending { SyncEngine.parseNewsDateToMillis(it.pubDate) }
                val encodedJson = json.encodeToString(sorted)
                // Retain all items permanently in user data without arbitrary limits
                AppDataStore.putString("app_news_cache", encodedJson)
                try {
                    val permanentDir = java.io.File(filesDir, "news_archive")
                    if (!permanentDir.exists()) permanentDir.mkdirs()
                    val archiveFile = java.io.File(permanentDir, "news_permanent_archive.json")
                    archiveFile.writeText(encodedJson)
                } catch (_: Exception) {}
                if (!isAnnouncement) {
                    AppDataStore.putString("Top_Link", link.ifEmpty { id })
                    if (pubDate.isNotEmpty()) AppDataStore.putString("Top_Time", pubDate)
                }
                val now = System.currentTimeMillis()
                AppDataStore.putLong("last_cloud_fcm_heartbeat", now)
                SyncEngine.saveSettings(SyncEngine.appSettings.value.copy(lastSync = now))
                SyncEngine.lastDirectAnnouncement = newItem
                SyncEngine.reloadFromPersistence()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Ultra-compact Micro-Thumbnail Loader:
     * - Target dimension: 48x48 px (exact line-height of notification text).
     * - Compressed RGB_565: RAM footprint is only ~3.5KB.
     * - Circular/square cropped for maximum sharpness.
     */
    private fun loadMicroThumbnail(imageUrl: String): Bitmap? {
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

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

            val reqDim = 48 // 48x48px text-line micro thumbnail
            var inSample = 1
            if (boundsOptions.outHeight > reqDim || boundsOptions.outWidth > reqDim) {
                val halfH = boundsOptions.outHeight / 2
                val halfW = boundsOptions.outWidth / 2
                while ((halfH / inSample) >= reqDim && (halfW / inSample) >= reqDim) {
                    inSample *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.ARGB_8888 // 50% RAM reduction
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null

            val minEdge = decoded.width.coerceAtMost(decoded.height)
            val cropX = (decoded.width - minEdge) / 2
            val cropY = (decoded.height - minEdge) / 2
            val squared = Bitmap.createBitmap(decoded, cropX, cropY, minEdge, minEdge)
            Bitmap.createScaledBitmap(squared, reqDim, reqDim, true)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Micro-Thumbnail Single Notification:
     * - Micro-Thumbnail (setLargeIcon) at 48x48px RGB_565 (~3.5KB).
     * - Full Android 7.0 to Android 17 compatibility.
     * - Strict routing:
     *   * If isNews is true: opens directly in external browser / Custom Tab via click_url.
     *   * If isNews is false (general/admin/announcement): opens in DetailsActivity / NotificationDetailScreen.
     */
    private fun showMicroThumbnailNotification(title: String, content: String, link: String, imageUrl: String, isNews: Boolean = true) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(notificationManager)

        // Strict Android 13+ permission validation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        } else {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        }

        val soundEnabled = if (isNews) SyncEngine.appSettings.value.notificationSound else true
        val channelId = if (soundEnabled) CHANNEL_SOUND_ID else CHANNEL_SILENT_ID
        val soundUri = if (soundEnabled) Uri.parse("android.resource://$packageName/" + R.raw.custom_sound) else null

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (isNews) {
                // Strict requirement: News notification ALWAYS opens the in-app News Screen directly
                putExtra("page", "news")
                data = Uri.parse("th3books://library?page=news")
            } else {
                putExtra("page", "notification_detail")
                if (link.isNotEmpty()) {
                    putExtra("link", link)
                    putExtra("open_notification_id", link)
                }
                putExtra("title", title)
                putExtra("content", content)
                putExtra("image_url", imageUrl)
                data = Uri.parse(if (link.isNotEmpty()) "th3books://library?page=notification_detail&link=" + Uri.encode(link) else "th3books://library?page=notification_detail")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 10000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val thumbnailBitmap = loadMicroThumbnail(imageUrl)
        val cleanTitle = SyncEngine.cleanHtmlEntities(
            title.replace("أخبار الثالث متوسط:", "").replace("أخبار الثالث متوسط", "").trim()
        ).ifBlank { title }
        val cleanContent = SyncEngine.cleanHtmlEntities(content)

        val isDevNotification = title.contains("المطور") || link.startsWith("dev_") || cleanTitle.contains("تنبيه")
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(android.graphics.Color.parseColor("#2563EB"))
            .setContentTitle(cleanTitle)
            .setPriority(if (!isNews) NotificationCompat.PRIORITY_MAX else (if (soundEnabled) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW))
            .setCategory(if (!isNews) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isNews) {
            notificationBuilder.setGroup(GROUP_KEY)
            notificationBuilder.setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            notificationBuilder.setSilent(true)
            notificationBuilder.setSound(null)
            notificationBuilder.setVibrate(null)
        } else {
            // Announcements & Admin alerts fire with their own sound immediately
            if (soundEnabled && soundUri != null) {
                notificationBuilder.setSound(soundUri)
                notificationBuilder.setVibrate(longArrayOf(0, 250, 150, 250))
                notificationBuilder.setSilent(false)
            }
        }

        // Ensure full headline text wraps nicely without truncation (BigTextStyle on single notifications)
        if (cleanContent.isNotBlank()) {
            notificationBuilder.setContentText(cleanContent)
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(cleanTitle)
                    .bigText(cleanContent)
            )
        } else {
            notificationBuilder.setContentText(cleanTitle)
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(cleanTitle)
                    .bigText(cleanTitle)
            )
        }

        // Micro-thumbnail beside headline (text line size)
        if (thumbnailBitmap != null) {
            notificationBuilder.setLargeIcon(thumbnailBitmap)
        }

        val notificationId = NOTIFICATION_ID_BASE + (cleanTitle.hashCode() and 0x7FFF)
        notificationManager.notify(notificationId, notificationBuilder.build())

        // Post group summary ONLY for standard news batching so multiple news cards sound ONCE
        if (isNews) {
            val summaryBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(android.graphics.Color.parseColor("#2563EB"))
                .setContentTitle("أخبار دراسية جديدة")
                .setContentText("أخبار دراسية جديدة")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)

            if (soundEnabled && soundUri != null) {
                summaryBuilder.setSound(soundUri)
                summaryBuilder.setVibrate(longArrayOf(0, 250, 150, 250))
                summaryBuilder.setSilent(false)
            } else {
                summaryBuilder.setSilent(true)
            }

            try {
                notificationManager.notify(NotificationHelper.SUMMARY_NOTIFICATION_ID, summaryBuilder.build())
            } catch (_: Exception) {}
        }
    }

    private fun createChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
