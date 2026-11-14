package com.Nightjar.Th3GradeLibraryKMP.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.BookStorageEngine
import com.Nightjar.Th3GradeLibraryKMP.ui.ToastManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

actual object BookDownloadManager {
    private const val CHANNEL_ID = "book_downloads_channel"
    const val NOTIFICATION_ID = 4040

    const val ACTION_PAUSE = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_PAUSE"
    const val ACTION_RESUME = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_RESUME"
    const val ACTION_CANCEL = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_CANCEL"
    const val ACTION_PAUSE_ALL = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_PAUSE_ALL"
    const val ACTION_CANCEL_ALL = "com.Nightjar.Th3GradeLibraryKMP.DOWNLOAD_CANCEL_ALL"
    const val EXTRA_ITEM_ID = "extra_download_item_id"

    private val HF_TOKEN = listOf("hf_", "DsqiUSeZnMrxEUSk", "XvYNTKmzXIyrNuCyMs").joinToString("")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val activeItems = ConcurrentHashMap<String, BookItem>()
    private val tempFiles = ConcurrentHashMap<String, File>()

    private val _itemStates = MutableStateFlow<Map<String, ItemDownloadState>>(emptyMap())
    actual val itemStates: StateFlow<Map<String, ItemDownloadState>> = _itemStates.asStateFlow()

    private val _isBatchDownloading = MutableStateFlow(false)
    actual val isBatchDownloading: StateFlow<Boolean> = _isBatchDownloading.asStateFlow()

    private val _batchProgress = MutableStateFlow(0f)
    actual val batchProgress: StateFlow<Float> = _batchProgress.asStateFlow()

    private val _activeNoticeMessage = MutableStateFlow<String?>(null)
    actual val activeNoticeMessage: StateFlow<String?> = _activeNoticeMessage.asStateFlow()

    private var noticeDismissJob: Job? = null
    private var appContext: Context? = null
    private var isReceiverRegistered = false

    private fun postActiveNotice(message: String?, autoDismissDelayMs: Long = 4000L) {
        noticeDismissJob?.cancel()
        _activeNoticeMessage.value = message
        if (message != null && autoDismissDelayMs > 0L) {
            noticeDismissJob = scope.launch {
                delay(autoDismissDelayMs)
                if (activeJobs.isEmpty() && !_isBatchDownloading.value) {
                    _activeNoticeMessage.value = null
                }
            }
        }
    }

    actual fun clearActiveNotice() {
        noticeDismissJob?.cancel()
        noticeDismissJob = null
        _activeNoticeMessage.value = null
    }

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PAUSE_ALL -> pauseAll()
                ACTION_CANCEL_ALL -> cancelAll()
                ACTION_PAUSE -> {
                    val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
                    val item = activeItems[itemId] ?: com.Nightjar.Th3GradeLibraryKMP.data.AllItems.getItemById(itemId) ?: return
                    pauseDownload(item)
                }
                ACTION_RESUME -> {
                    val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
                    val item = activeItems[itemId] ?: com.Nightjar.Th3GradeLibraryKMP.data.AllItems.getItemById(itemId) ?: return
                    context?.let { downloadItem(it, item) }
                }
                ACTION_CANCEL -> {
                    val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
                    val item = activeItems[itemId] ?: com.Nightjar.Th3GradeLibraryKMP.data.AllItems.getItemById(itemId) ?: return
                    if (_isBatchDownloading.value) {
                        cancelAll()
                    } else {
                        cancelDownload(item)
                    }
                }
            }
        }
    }

    private fun registerReceiverIfNeeded(context: Context) {
        if (!isReceiverRegistered) {
            try {
                val filter = IntentFilter().apply {
                    addAction(ACTION_PAUSE)
                    addAction(ACTION_RESUME)
                    addAction(ACTION_CANCEL)
                    addAction(ACTION_PAUSE_ALL)
                    addAction(ACTION_CANCEL_ALL)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.applicationContext.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    context.applicationContext.registerReceiver(downloadReceiver, filter)
                }
                isReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun startForegroundServiceIfNeeded(context: Context) {
        try {
            val intent = Intent().apply {
                setClassName(context.packageName, "com.Nightjar.Th3GradeLibraryKMP.BookDownloadForegroundService")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }

    private fun stopForegroundServiceIfDone(context: Context) {
        if (activeJobs.isEmpty() && !_isBatchDownloading.value) {
            try {
                val intent = Intent().apply {
                    setClassName(context.packageName, "com.Nightjar.Th3GradeLibraryKMP.BookDownloadForegroundService")
                }
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = cm.activeNetwork ?: return false
                val actNw = cm.getNetworkCapabilities(nw) ?: return false
                actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val ni = cm.activeNetworkInfo ?: return false
                @Suppress("DEPRECATION")
                ni.isConnected
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun updateDynamicProgress(ctx: Context, justFinishedItem: BookItem? = null) {
        val count = activeJobs.size
        if (count == 0) {
            if (justFinishedItem != null) {
                val typeLabel = if (!justFinishedItem.isNote) "الكتاب" else "الملزمة"
                val doneMsg = "اكتمل تنزيل $typeLabel: ${justFinishedItem.title} بنجاح ✓"
                postActiveNotice(doneMsg, 4000L)
                updateNotification(
                    ctx,
                    title = "اكتمل تنزيل: ${justFinishedItem.title} بنجاح ✓",
                    text = "جميع الملفات المحملة جاهزة أوفلاين في جهازك بدون إنترنت",
                    progress = 100,
                    itemId = null
                )
            } else if (!_isBatchDownloading.value) {
                postActiveNotice(null, 0L)
            }
            stopForegroundServiceIfDone(ctx)
            return
        }

        val items = activeJobs.keys.mapNotNull { activeItems[it] }
        if (items.isEmpty()) return

        if (items.size == 1) {
            val item = items.first()
            val state = _itemStates.value[item.id]
            val pct = ((state?.progress ?: 0f) * 100).toInt()
            val typeLabel = if (!item.isNote) "الكتاب" else "الملزمة"
            postActiveNotice("جاري تنزيل $typeLabel: ${item.title} ($pct%)", 0L)

            updateNotification(
                ctx,
                title = "جاري تحميل: ${item.title} ($pct%)",
                text = "جاري التحميل بأقصى سرعة...",
                progress = pct,
                itemId = item.id
            )
        } else {
            val titlesWithPcts = items.map { it to (((_itemStates.value[it.id]?.progress) ?: 0f) * 100).toInt() }
            val avgProgress = (titlesWithPcts.map { it.second }.average()).toInt()

            if (items.size == 2) {
                val (it1, pct1) = titlesWithPcts[0]
                val (it2, pct2) = titlesWithPcts[1]
                postActiveNotice("جاري تنزيل ملفين: ${it1.title} ($pct1%) • ${it2.title} ($pct2%)", 0L)
            } else {
                val (it1, pct1) = titlesWithPcts[0]
                postActiveNotice("جاري تنزيل ${items.size} ملفات: ${it1.title} ($pct1%) و${items.size - 1} ملفات أخرى ($avgProgress%)", 0L)
            }

            updateNotification(
                ctx,
                title = "جاري تنزيل ${items.size} ملفات ($avgProgress%)",
                text = items.joinToString(" • ") { "${it.title} (${((_itemStates.value[it.id]?.progress ?: 0f) * 100).toInt()}%)" },
                progress = avgProgress,
                itemId = null,
                isMultiDownload = true
            )
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تحميل الكتب والملازم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم وتحكم تحميل الكتب والملازم سحابياً"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun createTapPendingIntent(context: Context): PendingIntent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
        launchIntent.action = "th3books.intent.action.OPEN_LIBRARY"
        launchIntent.data = Uri.parse("th3books://library")
        launchIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(context, 100, launchIntent, flags)
    }

    private fun createActionPendingIntent(context: Context, action: String, itemId: String, requestCode: Int): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    fun createForegroundNotification(context: Context): Notification {
        ensureNotificationChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("جاري تنزيل الملفات...")
            .setContentText("المكتبة تعمل في الخلفية لإكمال التنزيل أوفلاين")
            .setContentIntent(createTapPendingIntent(context))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(
        context: Context,
        title: String,
        text: String,
        progress: Int,
        itemId: String? = null,
        isPaused: Boolean = false,
        isIndeterminate: Boolean = false,
        isMultiDownload: Boolean = false
    ) {
        try {
            ensureNotificationChannel(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val isDownloading = progress in 0..99 && !isPaused

            val icon = when {
                progress >= 100 -> android.R.drawable.stat_sys_download_done
                isPaused -> android.R.drawable.ic_media_pause
                progress < 0 -> android.R.drawable.stat_notify_error
                else -> android.R.drawable.stat_sys_download
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(createTapPendingIntent(context))
                .setOngoing(isDownloading)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setSubText(if (isDownloading && progress >= 0) "$progress%" else null)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

            if (isDownloading) {
                builder.setProgress(100, progress, isIndeterminate)
            } else {
                builder.setProgress(0, 0, false)
            }

            // Action buttons
            if (itemId != null) {
                if (isDownloading) {
                    val pauseIntent = createActionPendingIntent(context, ACTION_PAUSE, itemId, 201)
                    val cancelIntent = createActionPendingIntent(context, ACTION_CANCEL, itemId, 202)
                    builder.addAction(android.R.drawable.ic_media_pause, "إيقاف مؤقت", pauseIntent)
                    builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", cancelIntent)
                } else if (isPaused) {
                    val resumeIntent = createActionPendingIntent(context, ACTION_RESUME, itemId, 203)
                    val cancelIntent = createActionPendingIntent(context, ACTION_CANCEL, itemId, 204)
                    builder.addAction(android.R.drawable.ic_media_play, "استئناف", resumeIntent)
                    builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", cancelIntent)
                }
            } else if (isMultiDownload && isDownloading) {
                val pauseAllIntent = Intent(ACTION_PAUSE_ALL).apply { setPackage(context.packageName) }
                val cancelAllIntent = Intent(ACTION_CANCEL_ALL).apply { setPackage(context.packageName) }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
                val pPause = PendingIntent.getBroadcast(context, 205, pauseAllIntent, flags)
                val pCancel = PendingIntent.getBroadcast(context, 206, cancelAllIntent, flags)
                builder.addAction(android.R.drawable.ic_media_pause, "إيقاف الكل", pPause)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء الكل", pCancel)
            }

            nm.notify(NOTIFICATION_ID, builder.build())

            // Auto dismiss notification when 100% complete after 3.5 seconds
            if (progress >= 100) {
                scope.launch {
                    delay(3500)
                    if (activeJobs.isEmpty()) {
                        nm.cancel(NOTIFICATION_ID)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    actual fun getItemState(itemId: String): ItemDownloadState {
        return _itemStates.value[itemId] ?: ItemDownloadState(status = DownloadStatus.IDLE)
    }

    actual fun downloadItem(context: Any, item: BookItem) {
        val ctx = context as Context
        appContext = ctx.applicationContext
        registerReceiverIfNeeded(ctx)

        // Offline network check
        if (!isNetworkAvailable(ctx)) {
            ToastManager.showToast("تعذر بدء التحميل، يرجى التحقق من اتصالك بالإنترنت والمحاولة مجدداً")
            return
        }

        if (BookStorageEngine.isDownloaded(ctx, item)) {
            updateState(item.id, ItemDownloadState(status = DownloadStatus.COMPLETED, progress = 1f))
            return
        }

        activeJobs[item.id]?.cancel()
        activeItems[item.id] = item
        startForegroundServiceIfNeeded(ctx)

        val job = scope.launch {
            val storageDir = BookStorageEngine.getStorageDir(ctx)
            val tempTarget = File(storageDir, "partial_$item.id.tmp")
            tempFiles[item.id] = tempTarget
            val permanentFile = BookStorageEngine.getPermanentFile(ctx, item)

            try {
                val existingBytesOnStart = if (tempTarget.exists()) tempTarget.length() else 0L
                val metaSize = CloudCatalogManager.getMeta(item)?.file_size ?: -1L
                val initialProgress = if (metaSize > 0 && existingBytesOnStart > 0) (existingBytesOnStart.toFloat() / metaSize.toFloat()).coerceIn(0f, 0.99f) else 0f
                updateState(item.id, ItemDownloadState(
                    status = DownloadStatus.DOWNLOADING,
                    progress = initialProgress,
                    bytesDownloaded = existingBytesOnStart,
                    totalBytes = metaSize
                ))
                val itemType = if (!item.isNote) "الكتاب" else "الملزمة"
                val startMsg = if (existingBytesOnStart > 0) {
                    "جاري استئناف تنزيل $itemType: ${item.title}..."
                } else {
                    "جاري الاتصال بالسحابة لتنزيل $itemType: ${item.title}..."
                }
                postActiveNotice(startMsg, 0L)

                val candidateUrls = CloudCatalogManager.getStreamCandidateUrls(item)
                if (candidateUrls.isEmpty()) {
                    throw Exception("الرابط غير متوفر حالياً")
                }

                var downloadSuccess = false
                var lastErrorMessage = "فشل الاتصال بجميع الخوادم"

                // Triple failover with 2 round-robin retry cycles
                retryLoop@ for (cycle in 1..2) {
                    for (candidateUrl in candidateUrls) {
                        ensureActive()
                        try {
                            updateNotification(
                                ctx,
                                title = "جاري تحميل: ${item.title}",
                                text = "جاري الاتصال بالسحابة فائقة السرعة...",
                                progress = (initialProgress * 100).toInt(),
                                itemId = item.id
                            )

                            var currentUrl = candidateUrl
                            var redirectCount = 0
                            var conn: HttpURLConnection? = null
                            val existingBytes = if (tempTarget.exists()) tempTarget.length() else 0L

                            // Follow HTTP redirects cleanly (up to 5)
                            while (redirectCount < 5) {
                                conn = URL(currentUrl).openConnection() as HttpURLConnection
                                conn.connectTimeout = 20000
                                conn.readTimeout = 35000
                                conn.instanceFollowRedirects = false
                                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

                                if (currentUrl.contains("manahj.edu.iq")) {
                                    conn.setRequestProperty("Referer", "https://manahj.edu.iq/")
                                    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/pdf,*/*;q=0.8")
                                }

                                if (currentUrl.contains("huggingface.co")) {
                                    conn.setRequestProperty("Authorization", "Bearer $HF_TOKEN")
                                }

                                if (existingBytes > 0) {
                                    conn.setRequestProperty("Range", "bytes=$existingBytes-")
                                }

                                val status = conn.responseCode
                                if (status in listOf(HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP, 307, 308)) {
                                    val newUrl = conn.getHeaderField("Location")
                                    if (!newUrl.isNullOrBlank()) {
                                        currentUrl = newUrl
                                        redirectCount++
                                        conn.disconnect()
                                        continue
                                    }
                                }
                                break
                            }

                            val activeConn = conn ?: throw Exception("تعذر فتح الاتصال")
                            val contentType = activeConn.contentType ?: ""
                            val responseCode = activeConn.responseCode

                            val isPartial = (responseCode == 206)
                            val isOk = (responseCode == 200)

                            if ((isPartial || isOk) && !contentType.contains("text/html")) {
                                val isResumed = isPartial && existingBytes > 0
                                val totalBytes = if (isResumed) {
                                    val contentLen = activeConn.contentLengthLong
                                    if (contentLen > 0) existingBytes + contentLen else metaSize
                                } else {
                                    if (activeConn.contentLengthLong > 0) activeConn.contentLengthLong else metaSize
                                }

                                activeConn.inputStream.use { input ->
                                    FileOutputStream(tempTarget, isResumed).use { output ->
                                        val buffer = ByteArray(64 * 1024)
                                        var bytesCopied = if (isResumed) existingBytes else 0L
                                        var bytesRead: Int
                                        var lastReportTime = System.currentTimeMillis()

                                        while (input.read(buffer).also { bytesRead = it } != -1) {
                                            ensureActive()
                                            output.write(buffer, 0, bytesRead)
                                            bytesCopied += bytesRead

                                            val progress = if (totalBytes > 0) (bytesCopied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                                            val now = System.currentTimeMillis()
                                            if (now - lastReportTime > 250) {
                                                lastReportTime = now
                                                updateState(item.id, ItemDownloadState(
                                                    status = DownloadStatus.DOWNLOADING,
                                                    progress = progress,
                                                    bytesDownloaded = bytesCopied,
                                                    totalBytes = totalBytes
                                                ))
                                                updateDynamicProgress(ctx)
                                            }
                                        }
                                    }
                                }

                                if (tempTarget.exists() && tempTarget.length() > 1024L) {
                                    if (permanentFile.exists()) {
                                        permanentFile.delete()
                                    }
                                    tempTarget.renameTo(permanentFile)
                                    updateState(item.id, ItemDownloadState(status = DownloadStatus.COMPLETED, progress = 1f))
                                    downloadSuccess = true
                                    activeJobs.remove(item.id)
                                    updateDynamicProgress(ctx, justFinishedItem = item)
                                    break@retryLoop
                                } else {
                                    throw Exception("الملف المحمل غير مكتمل")
                                }
                            } else {
                                throw Exception("استجابة غير صالحة من الخادم: $responseCode")
                            }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (e: Exception) {
                            lastErrorMessage = e.message ?: "فشل الخادم"
                            delay(1000)
                        }
                    }
                }

                if (!downloadSuccess) {
                    throw Exception(lastErrorMessage)
                }

            } catch (ce: CancellationException) {
                val typeLabel = if (!item.isNote) "الكتاب" else "الملزمة"
                val pauseMsg = "تم إيقاف تنزيل $typeLabel: ${item.title} مؤقتاً"
                postActiveNotice(pauseMsg, 4000L)
                val currentBytes = if (tempTarget.exists()) tempTarget.length() else 0L
                val currentProgress = _itemStates.value[item.id]?.progress ?: 0f
                updateState(item.id, ItemDownloadState(status = DownloadStatus.PAUSED, progress = currentProgress, bytesDownloaded = currentBytes))
                updateNotification(
                    ctx,
                    title = "تم إيقاف تحميل: ${item.title}",
                    text = "تم إيقاف عملية التحميل مؤقتاً ويمكن استئنافها في أي وقت",
                    progress = -1,
                    itemId = item.id,
                    isPaused = true
                )
            } catch (e: Exception) {
                val errReason = if (e.message?.contains("غير متوفر") == true) "الرابط غير متوفر حالياً" else "تعذر التنزيل، يرجى التحقق من الاتصال والمحاولة لاحقاً"
                updateState(item.id, ItemDownloadState(status = DownloadStatus.FAILED, error = errReason))
                val typeLabel = if (!item.isNote) "الكتاب" else "الملزمة"
                val failMsg = "تعذر تنزيل $typeLabel: ${item.title} ($errReason) ✕"
                postActiveNotice(failMsg, 4000L)
                updateNotification(
                    ctx,
                    title = "فشل تحميل: ${item.title}",
                    text = errReason,
                    progress = -1,
                    itemId = item.id
                )
            } finally {
                activeJobs.remove(item.id)
                tempFiles.remove(item.id)
                stopForegroundServiceIfDone(ctx)
            }
        }
        activeJobs[item.id] = job
    }

    actual fun pauseDownload(item: BookItem) {
        val job = activeJobs.remove(item.id)
        job?.cancel()
        val currentProgress = _itemStates.value[item.id]?.progress ?: 0f
        updateState(item.id, ItemDownloadState(status = DownloadStatus.PAUSED, progress = currentProgress))
        appContext?.let { ctx ->
            val typeLabel = if (!item.isNote) "الكتاب" else "الملزمة"
            postActiveNotice("تم إيقاف تنزيل $typeLabel: ${item.title} مؤقتاً", 4000L)
            updateNotification(
                ctx,
                title = "تم إيقاف تحميل: ${item.title}",
                text = "تم إيقاف عملية التحميل مؤقتاً",
                progress = -1,
                itemId = item.id,
                isPaused = true
            )
            stopForegroundServiceIfDone(ctx)
        }
    }

    actual fun pauseAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        postActiveNotice("تم إيقاف جميع التنزيلات مؤقتاً", 4000L)
        appContext?.let { ctx ->
            updateNotification(
                ctx,
                title = "تم إيقاف التنزيلات مؤقتاً",
                text = "تم إيقاف عمليات التنزيل الجارية مؤقتاً",
                progress = -1,
                itemId = null,
                isPaused = true
            )
            stopForegroundServiceIfDone(ctx)
        }
    }

    actual fun cancelDownload(item: BookItem) {
        val job = activeJobs.remove(item.id)
        job?.cancel()
        tempFiles.remove(item.id)?.delete()
        activeItems.remove(item.id)
        updateState(item.id, ItemDownloadState(status = DownloadStatus.IDLE, progress = 0f))
        if (_isBatchDownloading.value) {
            _isBatchDownloading.value = false
        }
        appContext?.let { ctx ->
            val typeLabel = if (!item.isNote) "الكتاب" else "الملزمة"
            postActiveNotice("تم إلغاء تنزيل $typeLabel: ${item.title}", 4000L)
            updateDynamicProgress(ctx)
            if (activeJobs.isEmpty() && !_isBatchDownloading.value) {
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIFICATION_ID)
            }
            stopForegroundServiceIfDone(ctx)
        }
    }

    actual fun downloadAll(context: Any, items: List<BookItem>) {
        val ctx = context as Context
        appContext = ctx.applicationContext
        registerReceiverIfNeeded(ctx)

        if (!isNetworkAvailable(ctx)) {
            ToastManager.showToast("تعذر بدء التحميل، يرجى التحقق من اتصالك بالإنترنت والمحاولة مجدداً")
            return
        }

        if (_isBatchDownloading.value) return
        _isBatchDownloading.value = true
        val specialTitle = "جاري تنزيل كتب وملازم الثالث متوسط الان لتكون اوف لاين بدون انترنت ..انتظر"
        postActiveNotice(specialTitle, 0L)
        startForegroundServiceIfNeeded(ctx)

        scope.launch {
            try {
                val undownloaded = items.filter { !BookStorageEngine.isDownloaded(ctx, it) }
                val totalCount = undownloaded.size
                if (totalCount == 0) {
                    postActiveNotice("جميع كتب وملازم الثالث متوسط محملة بالفعل وجاهزة أوفلاين ✓", 4000L)
                    ToastManager.showToast("جميع الكتب والملازم محملة بالفعل وجاهزة أوفلاين")
                    _isBatchDownloading.value = false
                    stopForegroundServiceIfDone(ctx)
                    return@launch
                }
                var completedCount = 0

                for (item in undownloaded) {
                    if (!_isBatchDownloading.value) break
                    downloadItem(ctx, item)
                    activeJobs[item.id]?.join()
                    completedCount++
                    val pct = (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                    _batchProgress.value = pct
                    val pctInt = (pct * 100).toInt()
                    postActiveNotice("جاري تنزيل كتب وملازم الثالث متوسط الان لتكون اوف لاين بدون انترنت ..انتظر ($pctInt%)", 0L)
                    updateNotification(
                        ctx,
                        title = "جاري تنزيل كتب وملازم الثالث متوسط الان لتكون اوف لاين بدون انترنت ..انتظر",
                        text = "جاري التنزيل: $pctInt% (مكتمل $completedCount من $totalCount)",
                        progress = pctInt
                    )
                }

                postActiveNotice("اكتملت جميع تنزيلات كتب وملازم الثالث متوسط بنجاح أوفلاين بدون إنترنت ✓", 4000L)
                updateNotification(
                    ctx,
                    title = "اكتملت جميع التنزيلات بنجاح",
                    text = "جميع كتب وملازم الثالث متوسط أصبحت جاهزة الآن أوفلاين بدون إنترنت!",
                    progress = 100
                )
            } finally {
                _isBatchDownloading.value = false
                stopForegroundServiceIfDone(ctx)
            }
        }
    }

    actual fun cancelAll() {
        _isBatchDownloading.value = false
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        tempFiles.values.forEach { it.delete() }
        tempFiles.clear()
        activeItems.clear()
        postActiveNotice("تم إلغاء جميع التنزيلات", 4000L)
        appContext?.let { ctx ->
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            stopForegroundServiceIfDone(ctx)
        }
    }

    private fun updateState(itemId: String, state: ItemDownloadState) {
        val current = _itemStates.value.toMutableMap()
        current[itemId] = state
        _itemStates.value = current
    }
}
