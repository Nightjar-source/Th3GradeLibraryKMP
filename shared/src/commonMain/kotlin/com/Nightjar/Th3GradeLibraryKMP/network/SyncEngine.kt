package com.Nightjar.Th3GradeLibraryKMP.network

import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.Nightjar.Th3GradeLibraryKMP.data.NewsItem
import com.Nightjar.Th3GradeLibraryKMP.data.AppSettings
import com.Nightjar.Th3GradeLibraryKMP.data.PlatformActionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.prof18.rssparser.RssParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncEngine {
    private val settings = AppDataStore
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient()

    var platformActionHandler: PlatformActionHandler? = null
    var isUserOnNewsScreen: Boolean = false

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    fun checkNotificationPermission() {
        _notificationsEnabled.value = platformActionHandler?.areNotificationsEnabled() ?: true
    }

    private val _newsList = MutableStateFlow<List<NewsItem>>(emptyList())
    val newsList = _newsList.asStateFlow()
    var lastDirectAnnouncement: NewsItem? = null

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings = _appSettings.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()

    // -1 means no fetch yet, 0 means "no new", >0 means count of new items
    private val _lastFetchedCount = MutableStateFlow(-1)
    val lastFetchedCount = _lastFetchedCount.asStateFlow()

    private val _savedBookmarks = MutableStateFlow<Set<String>>(emptySet())
    val savedBookmarks = _savedBookmarks.asStateFlow()

    private val _lastReadPages = MutableStateFlow<Map<String, Int>>(emptyMap())
    val lastReadPages = _lastReadPages.asStateFlow()

    private fun loadInitialExpandedBookmarks(): Set<String> {
        return try {
            val str = settings.getString("expanded_bookmarks", "")
            if (str.isNotBlank()) str.split(",").filter { it.isNotBlank() }.toSet() else emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private val _expandedBookmarks = MutableStateFlow<Set<String>>(loadInitialExpandedBookmarks())
    val expandedBookmarks = _expandedBookmarks.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView = _isGridView.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _readNewsIds = MutableStateFlow<Set<String>>(emptySet())
    val readNewsIds = _readNewsIds.asStateFlow()

    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded = _isDataLoaded.asStateFlow()

    init {
        loadLocalSettings()
        loadLocalNews()
        loadBookmarks()
        loadLastReadPages()
        loadExpandedBookmarks()
        loadGridView()
        loadSelectedCategory()
        loadReadNews()
        _isDataLoaded.value = true
    }

    fun reloadFromPersistence() {
        loadLocalSettings()
        loadLocalNews()
        loadBookmarks()
        loadLastReadPages()
        loadExpandedBookmarks()
        loadGridView()
        loadReadNews()
        _isDataLoaded.value = true
    }

    private fun loadReadNews() {
        try {
            val readStr = settings.getString("read_news_ids", "")
            if (readStr.isNotEmpty()) {
                if (readStr.trim().startsWith("[")) {
                    _readNewsIds.value = json.decodeFromString<List<String>>(readStr).toSet()
                } else {
                    _readNewsIds.value = readStr.split(",").filter { it.isNotEmpty() }.toSet()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun markNewsAsRead(id: String) {
        if (id.isEmpty()) return
        val current = _readNewsIds.value.toMutableSet()
        if (!current.contains(id)) {
            current.add(id)
            _readNewsIds.value = current
            try {
                settings.putString("read_news_ids", json.encodeToString(current.toList()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadExpandedBookmarks() {
        try {
            val expandedStr = settings.getString("expanded_bookmarks", "")
            if (expandedStr.isNotEmpty()) {
                _expandedBookmarks.value = expandedStr.split(",").toSet()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleExpandedBookmark(key: String) {
        val current = _expandedBookmarks.value.toMutableSet()
        if (current.contains(key)) {
            current.remove(key)
        } else {
            current.add(key)
        }
        _expandedBookmarks.value = current
        settings.putString("expanded_bookmarks", current.joinToString(","))
    }

    private fun loadGridView() {
        _isGridView.value = settings.getBoolean("is_grid_view", true)
    }

    fun saveGridView(isGrid: Boolean) {
        _isGridView.value = isGrid
        settings.putBoolean("is_grid_view", isGrid)
    }

    private fun loadSelectedCategory() {
        // Always start fresh on the main Library page on fresh app launches
        _selectedCategory.value = null
    }

    fun saveSelectedCategory(cat: String?) {
        _selectedCategory.value = cat
        settings.putString("selected_category", cat ?: "")
    }

    private fun loadLastReadPages() {
        try {
            val lastPagesStr = settings.getString("last_read_pages", "")
            if (lastPagesStr.isNotEmpty()) {
                val map = mutableMapOf<String, Int>()
                lastPagesStr.split(",").forEach {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        map[parts[0]] = parts[1].toIntOrNull() ?: 0
                    }
                }
                _lastReadPages.value = map
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveLastReadPage(pdfId: String, pageIndex: Int) {
        val current = _lastReadPages.value.toMutableMap()
        current[pdfId] = pageIndex
        _lastReadPages.value = current
        val serialized = current.entries.joinToString(",") { "${it.key}:${it.value}" }
        settings.putString("last_read_pages", serialized)
    }

    private fun loadBookmarks() {
        try {
            val bookmarksStr = settings.getString("saved_bookmarks", "")
            if (bookmarksStr.isNotEmpty()) {
                _savedBookmarks.value = bookmarksStr.split(",").toSet()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleBookmark(key: String): Boolean {
        val current = _savedBookmarks.value.toMutableSet()
        val isSaved = if (current.contains(key)) {
            current.remove(key)
            false
        } else {
            current.add(key)
            true
        }
        _savedBookmarks.value = current
        settings.putString("saved_bookmarks", current.joinToString(","))
        return isSaved
    }

    private fun loadLocalSettings() {
        try {
            val theme = settings.getString("theme", "system")
            val useMaterialYou = settings.getBoolean("useMaterialYou", false)
            val primaryColor = settings.getString("primaryColor", "#2563eb")
            val syncInterval = settings.getString("syncInterval", "30m")
            val cacheClearInterval = settings.getString("cacheClearInterval", "6m")
            val notificationSound = settings.getBoolean("notificationSound", false)
            val lastSync = settings.getLong("lastSync", 0L)
            val pureWhiteMode = settings.getBoolean("pureWhiteMode", false)
            val pdfScrollDirection = settings.getString("pdfScrollDirection", "vertical")

            val isOnboardingCompleted = settings.getBoolean("isOnboardingCompleted", false)
            val autoDownloadOnUpdate = settings.getBoolean("autoDownloadOnUpdate", false)

            _appSettings.value = AppSettings(
                theme = theme,
                pureBlackMode = settings.getBoolean("pureBlackMode", false),
                pureWhiteMode = pureWhiteMode,
                useMaterialYou = useMaterialYou,
                primaryColor = primaryColor,
                pdfScrollDirection = pdfScrollDirection,
                syncInterval = syncInterval,
                cacheClearInterval = cacheClearInterval,
                notificationSound = notificationSound,
                lastSync = lastSync,
                isOnboardingCompleted = isOnboardingCompleted,
                autoDownloadOnUpdate = autoDownloadOnUpdate
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSettings(newSettings: AppSettings) {
        _appSettings.value = newSettings
        settings.putString("theme", newSettings.theme)
        settings.putBoolean("pureBlackMode", newSettings.pureBlackMode)
        settings.putBoolean("pureWhiteMode", newSettings.pureWhiteMode)
        settings.putBoolean("useMaterialYou", newSettings.useMaterialYou)
        settings.putString("primaryColor", newSettings.primaryColor)
        settings.putString("pdfScrollDirection", newSettings.pdfScrollDirection)
        settings.putString("syncInterval", newSettings.syncInterval)
        settings.putString("cacheClearInterval", newSettings.cacheClearInterval)
        settings.putBoolean("notificationSound", newSettings.notificationSound)
        settings.putLong("lastSync", newSettings.lastSync)
        settings.putBoolean("isOnboardingCompleted", newSettings.isOnboardingCompleted)
        settings.putBoolean("autoDownloadOnUpdate", newSettings.autoDownloadOnUpdate)
        platformActionHandler?.rescheduleBackgroundSync()
        if (newSettings.syncInterval == "on_open") {
            platformActionHandler?.unsubscribeFromNewsTopic()
        } else {
            platformActionHandler?.subscribeToNewsTopic()
        }
    }

    fun completeOnboarding() {
        val current = _appSettings.value
        val updated = current.copy(isOnboardingCompleted = true)
        saveSettings(updated)
    }

    private fun loadLocalNews() {
        try {
            val cachedNews = settings.getString("app_news_cache", "")
            if (cachedNews.isNotEmpty()) {
                val list = json.decodeFromString<List<NewsItem>>(cachedNews).map {
                    if (it.formattedDate.isEmpty() && it.pubDate.isNotEmpty()) {
                        it.copy(formattedDate = formatNewsDate(it.pubDate))
                    } else it
                }
                val sorted = list.sortedByDescending { parseNewsDateToMillis(it.pubDate) }
                _newsList.value = sorted
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractImageFromContent(content: String): String? {
        val regex = Regex("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>")
        val match = regex.find(content)
        val raw = match?.groups?.get(1)?.value ?: return null
        return if (raw.contains("blogger.googleusercontent.com") || raw.contains("bp.blogspot.com")) {
            raw.replace("/s72-c/", "/s1600/").replace("/w72-h72/", "/s1600/")
        } else {
            raw
        }
    }

    private fun saveLocalNews(list: List<NewsItem>) {
        val sorted = list.sortedByDescending { parseNewsDateToMillis(it.pubDate) }
        _newsList.value = sorted
        try {
            settings.putString("app_news_cache", json.encodeToString(sorted))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recordDirectAnnouncement(title: String, content: String, imageUrl: String, link: String): NewsItem? {
        return try {
            val id = if (link.isNotBlank() && !link.startsWith("th3books://")) link else ("announcement_" + title.hashCode() + "_" + (ClockSystem.currentTimeMillis() / 1000))
            val nowMs = ClockSystem.currentTimeMillis()
            val nowStr = nowMs.toString()
            loadLocalNews()
            val existing = _newsList.value.toMutableList()
            val alreadyPresent = existing.find { 
                it.id == id || (title.isNotBlank() && it.title == title && it.contentSnippet == content) 
            }
            if (alreadyPresent != null) {
                lastDirectAnnouncement = alreadyPresent
                alreadyPresent
            } else {
                val item = NewsItem(
                    id = id,
                    title = title,
                    link = link,
                    contentSnippet = content,
                    pubDate = nowStr,
                    imageUrl = imageUrl.ifBlank { null },
                    formattedDate = formatNewsDate(nowStr),
                    isAnnouncement = true
                )
                existing.add(0, item)
                val sorted = existing.sortedByDescending { parseNewsDateToMillis(it.pubDate) }
                saveLocalNews(sorted)
                lastDirectAnnouncement = item
                item
            }
        } catch (_: Exception) { null }
    }

    private fun extractDomain(url: String): String {
        return try {
            val withoutProtocol = if (url.contains("://")) {
                url.substringAfter("://")
            } else {
                url
            }
            withoutProtocol.substringBefore("/")
        } catch (e: Exception) {
            ""
        }
    }

    fun clearLastFetchedCount() {
        _lastFetchedCount.value = -1
    }

    fun normalizeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        var clean = url.trim()
        clean = clean.replace(Regex("^https?://", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("^www\\.", RegexOption.IGNORE_CASE), "")
        val qIdx = clean.indexOf('?')
        if (qIdx != -1) clean = clean.substring(0, qIdx)
        val hIdx = clean.indexOf('#')
        if (hIdx != -1) clean = clean.substring(0, hIdx)
        return clean.trimEnd('/')
    }

    fun cleanHtmlEntities(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&#8220;", "\"")
            .replace("&#8221;", "\"")
            .replace("&#8216;", "'")
            .replace("&#8217;", "'")
            .replace("&#8211;", "-")
            .replace("&#8212;", "—")
            .replace(Regex("<[^>]*>"), "")
            .trim()
    }

    fun formatNewsDate(pubDate: String): String = try {
        val num = pubDate.toLongOrNull()
        if (num != null) {
            val sec = (num / 1000L)
            val timeInDay = (sec % 86400L)
            var hour = ((timeInDay / 3600L) + 3).toInt() % 24
            val min = ((timeInDay % 3600L) / 60L).toInt()
            val amPm = if (hour >= 12) "م" else "ص"
            if (hour > 12) hour -= 12
            if (hour == 0) hour = 12
            val minStr = if (min < 10) "0$min" else "$min"
            "الآن  $hour:$minStr $amPm"
        } else {
            val dParts = pubDate.split(" ")
            if (dParts.size >= 5) {
                val day = dParts[1]
                val month = when (dParts[2].lowercase()) {
                    "jan" -> "1"; "feb" -> "2"; "mar" -> "3"; "apr" -> "4"
                    "may" -> "5"; "jun" -> "6"; "jul" -> "7"; "aug" -> "8"
                    "sep" -> "9"; "oct" -> "10"; "nov" -> "11"; "dec" -> "12"
                    else -> "1"
                }
                val year = dParts[3]
                val timeParts = dParts[4].split(":")
                var hour = timeParts[0].toIntOrNull() ?: 12
                val min = timeParts[1]
                
                // تعديل التوقيت ليكون بتوقيت العراق (+3)
                hour += 3
                if (hour >= 24) hour -= 24
                
                val amPm = if (hour >= 12) "م" else "ص"
                if (hour > 12) hour -= 12
                if (hour == 0) hour = 12
                "$day-$month-$year  $hour:$min $amPm"
            } else if (pubDate.contains("-") && pubDate.contains(":")) {
                val p = pubDate.split(" ")
                val ymd = p[0]
                val t = p.getOrNull(1)?.split(":") ?: listOf("12", "00")
                var h = t[0].toIntOrNull() ?: 12
                val m = t.getOrNull(1) ?: "00"
                val amPm = if (h >= 12) "م" else "ص"
                if (h > 12) h -= 12
                if (h == 0) h = 12
                "$ymd  $h:$m $amPm"
            } else pubDate
        }
    } catch (e: Exception) { pubDate }

    fun parseNewsDateToMillis(pubDate: String): Long = try {
        val num = pubDate.toLongOrNull()
        if (num != null) return num

        if (pubDate.contains("-") && pubDate.contains(":")) {
            val parts = pubDate.split(" ")
            val ymd = parts[0].split("-")
            val hms = parts.getOrNull(1)?.split(":") ?: listOf("0", "0", "0")
            val y = ymd.getOrNull(0)?.toIntOrNull() ?: 2026
            val m = ymd.getOrNull(1)?.toIntOrNull() ?: 1
            val d = ymd.getOrNull(2)?.toIntOrNull() ?: 1
            val h = hms.getOrNull(0)?.toIntOrNull() ?: 0
            val min = hms.getOrNull(1)?.toIntOrNull() ?: 0
            val s = hms.getOrNull(2)?.toIntOrNull() ?: 0
            val daysApprox = (y - 1970) * 365L + ((y - 1970) / 4) + (m - 1) * 30L + d
            return (daysApprox * 86400L + h * 3600L + min * 60L + s) * 1000L
        }

        val dParts = pubDate.split(" ")
        if (dParts.size >= 5) {
            val day = dParts[1].toIntOrNull() ?: 1
            val month = when (dParts[2].lowercase()) {
                "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4
                "may" -> 5; "jun" -> 6; "jul" -> 7; "aug" -> 8
                "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
                else -> 1
            }
            val year = dParts[3].toIntOrNull() ?: 2026
            val timeParts = dParts[4].split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
            val min = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
            val sec = timeParts.getOrNull(2)?.toIntOrNull() ?: 0
            val daysApprox = (year - 1970) * 365L + ((year - 1970) / 4) + (month - 1) * 30L + day
            (daysApprox * 86400L + hour * 3600L + min * 60L + sec) * 1000L
        } else {
            0L
        }
    } catch (_: Exception) { 0L }

    suspend fun fetchAndSync(forced: Boolean = false): Int = withContext(Dispatchers.Default) {
        if (_syncing.value) return@withContext 0 // Sync Lock: exit immediately if a sync is already in progress
        val now = ClockSystem.currentTimeMillis()
        val lastSync = _appSettings.value.lastSync
        val lastFcm = AppDataStore.getLong("last_cloud_fcm_heartbeat", 0L)
        val mostRecentTouch = maxOf(lastSync, lastFcm)
        val timeSinceSync = now - mostRecentTouch

        val intervalMs = when (_appSettings.value.syncInterval) {
            "on_open" -> 0L
            "15m"     -> 15 * 60 * 1000L
            "30m"     -> 30 * 60 * 1000L
            "1h"      -> 60 * 60 * 1000L
            "6h"      -> 6 * 60 * 60 * 1000L
            "12h"     -> 12 * 60 * 60 * 1000L
            "24h"     -> 24 * 60 * 60 * 1000L
            else      -> 30 * 60 * 1000L // default 30m
        }
        if (!forced && intervalMs > 0L && timeSinceSync < intervalMs) {
            return@withContext 0
        }

        // 1. We removed the 10-minute barrier in favor of the Dead Man's Switch architecture.
        // The Alarm will be dynamically pushed forward by WorkManager upon success.

        _syncing.value = true
        _lastFetchedCount.value = -1 // Reset fetch result state before starting

        // Refresh dynamic cloud catalog metadata asynchronously from GitHub/CDN
        try {
            CloudCatalogManager.refreshManifest()
        } catch (_: Exception) {}
        try {
            val rssParser = RssParser()
            val cfWorkerUrl = "https://th3books-gateway.karrar1iq.workers.dev/news"
            val originalUrl = "https://feeds.feedburner.com/karraraliraqii/gkhbdxas1m0"

            suspend fun fetchWithFallbacks(): com.prof18.rssparser.model.RssChannel {
                val targets = listOf(
                    cfWorkerUrl,
                    originalUrl,
                    "https://api.allorigins.win/raw?url=$originalUrl",
                    "https://corsproxy.io/?url=$originalUrl",
                    "https://api.codetabs.com/v1/proxy/?quest=$originalUrl"
                )
                for (target in targets) {
                    try {
                        return kotlinx.coroutines.withTimeout(7000L) {
                            val channel = rssParser.getRssChannel(target)
                            if (channel.items.isNotEmpty()) channel else throw Exception("Empty channel")
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException && e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                    }
                }
                throw Exception("تعذر الاتصال بجميع خوادم الأخبار")
            }

            val channel = fetchWithFallbacks()

            val fetchedItems = channel.items.map { item ->
                val pubDate = item.pubDate ?: ""
                val cleanTitle = cleanHtmlEntities(item.title ?: "")
                val cleanSnippet = ""
                val link = item.link ?: ""
                val id = if (link.isNotBlank()) link else (item.guid ?: ("news_" + cleanTitle.hashCode()))
                NewsItem(
                    id = id,
                    title = cleanTitle,
                    link = link,
                    contentSnippet = cleanSnippet,
                    pubDate = pubDate,
                    imageUrl = item.image ?: extractImageFromContent(item.content ?: item.description ?: ""),
                    formattedDate = formatNewsDate(pubDate),
                    isAnnouncement = false
                )
            }
            
            if (fetchedItems.isNotEmpty()) {
                // ── Dual Boundary Algorithm (Top & Bottom Links/Times) ──
                val topLink = settings.getString("Top_Link", "")
                val topTime = settings.getString("Top_Time", "")
                val bottomLink = settings.getString("Bottom_Link", "")
                val bottomTime = settings.getString("Bottom_Time", "")

                val newUniqueItems = mutableListOf<NewsItem>()

                // Detect domain change to treat as a fresh run
                val firstItemLink = fetchedItems.firstOrNull()?.link ?: ""
                val domainChanged = topLink.isNotEmpty() && firstItemLink.isNotEmpty() && 
                        extractDomain(topLink) != extractDomain(firstItemLink)

                val isListEmpty = _newsList.value.isEmpty()

                if (topLink.isEmpty() || domainChanged || isListEmpty) {
                    // First run, domain changed, or empty cache/list: take latest 15 baseline items
                    val validItems = fetchedItems.take(15)
                    newUniqueItems.addAll(validItems)
                    
                    if (validItems.isNotEmpty()) {
                        settings.putString("Top_Link", validItems.first().link)
                        settings.putString("Top_Time", validItems.first().pubDate)
                        settings.putString("Bottom_Link", validItems.last().link)
                        settings.putString("Bottom_Time", validItems.last().pubDate)
                    }
                } else {
                    val collectedItems = mutableListOf<NewsItem>()
                    var matchedBoundary = false
                    for (item in fetchedItems) {
                        // Found exact Top boundary -> Everything below is old
                        if (item.link == topLink && item.pubDate == topTime) {
                            matchedBoundary = true
                            break
                        }
                        // Top broken, found Bottom boundary -> Everything below is definitely old
                        if (item.link == bottomLink && item.pubDate == bottomTime) {
                            matchedBoundary = true
                            break
                        }

                        collectedItems.add(item)
                    }
                    if (!matchedBoundary && collectedItems.isEmpty()) {
                        collectedItems.addAll(fetchedItems)
                    }

                    // Filter to find TRULY new or updated items
                    val trulyNewOrUpdated = collectedItems.filter { fetchedItem ->
                        val normFetched = normalizeUrl(fetchedItem.link)
                        val localItem = _newsList.value.find {
                            it.id == fetchedItem.id ||
                            (normFetched.isNotEmpty() && normalizeUrl(it.link) == normFetched) ||
                            (it.title.isNotBlank() && fetchedItem.title.isNotBlank() && it.title == fetchedItem.title)
                        }
                        // New if not in local DB. Updated if in local DB but time changed.
                        localItem == null || localItem.pubDate != fetchedItem.pubDate
                    }

                    newUniqueItems.addAll(trulyNewOrUpdated)

                    if (newUniqueItems.isNotEmpty()) {
                        // Dynamic sliding window: Old Top becomes New Bottom, Absolute Newest becomes New Top
                        val absoluteNewest = fetchedItems.first()
                        val isFirstRunOrCleared = topLink.isEmpty() || isListEmpty || domainChanged
                        val newBottomLink = if (isFirstRunOrCleared) newUniqueItems.last().link else topLink
                        val newBottomTime = if (isFirstRunOrCleared) newUniqueItems.last().pubDate else topTime
                        
                        settings.putString("Bottom_Link", newBottomLink)
                        settings.putString("Bottom_Time", newBottomTime)
                        settings.putString("Top_Link", absoluteNewest.link)
                        settings.putString("Top_Time", absoluteNewest.pubDate)
                    } else if (topLink.isNotEmpty() && fetchedItems.none { it.link == topLink || normalizeUrl(it.link) == normalizeUrl(topLink) }) {
                        // The previous Top article was deleted from the feed: realign Top_Link to current feed head
                        val currentTop = fetchedItems.first()
                        settings.putString("Top_Link", currentTop.link)
                        settings.putString("Top_Time", currentTop.pubDate)
                    }
                }

                if (newUniqueItems.isNotEmpty()) {

                    // If any of the new/updated items were previously marked as read, mark them as unread again
                    val currentReadIds = _readNewsIds.value.toMutableSet()
                    var readIdsChanged = false
                    newUniqueItems.forEach { newItem ->
                        val oldItem = _newsList.value.find { it.id == newItem.id }
                        if (oldItem != null && oldItem.pubDate != newItem.pubDate) {
                            if (currentReadIds.remove(newItem.id)) {
                                readIdsChanged = true
                            }
                        }
                    }
                    if (readIdsChanged) {
                        _readNewsIds.value = currentReadIds
                        try {
                            settings.putString("read_news_ids", json.encodeToString(currentReadIds.toList()))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    // Reload latest local news/announcements so background FCM pushes are never lost
                    loadLocalNews()
                    // Merge new items on top, de-duplicate by normalized key (preserving all distinct announcements)
                    val combined = (newUniqueItems + _newsList.value).distinctBy {
                        if (it.isAnnouncement || it.link.startsWith("th3books://")) {
                            "${it.id}_${it.title}_${it.contentSnippet.hashCode()}"
                        } else {
                            val norm = normalizeUrl(it.link)
                            if (norm.isNotEmpty()) norm else (it.title.ifBlank { it.id })
                        }
                    }

                    // Retain all items permanently in user data without arbitrary count limits
                    val cacheClear = _appSettings.value.cacheClearInterval
                    val filteredList = if (cacheClear == "never") {
                        combined
                    } else {
                        val limitDays = when (cacheClear) {
                            "7d"   -> 7
                            "1m"   -> 30
                            "3m"   -> 90
                            "6m"   -> 180
                            else   -> null
                        }
                        if (limitDays != null) {
                            val cutoffMs = System.currentTimeMillis() - (limitDays.toLong() * 24 * 60 * 60 * 1000L)
                            combined.filter { item ->
                                val parsed = try { parseNewsDateToMillis(item.pubDate) } catch (_: Exception) { 0L }
                                parsed == 0L || parsed >= cutoffMs
                            }
                        } else {
                            combined
                        }
                    }

                    saveLocalNews(filteredList)
                    _lastFetchedCount.value = newUniqueItems.size
                    val isFirstRunOrCleared = topLink.isEmpty() || isListEmpty || domainChanged
                    if (isFirstRunOrCleared && newUniqueItems.isNotEmpty()) {
                        // Launch initial notification on first run after onboarding finishes
                        if (platformActionHandler?.areNotificationsEnabled() == true) {
                            platformActionHandler?.showNewsNotification(newUniqueItems.map { it.title }, newUniqueItems.size)
                        }
                    } else if (newUniqueItems.isNotEmpty()) {
                        // Launch system notification ONLY if user is not currently viewing the News screen
                        if (!isUserOnNewsScreen) {
                            if (_appSettings.value.notificationSound && platformActionHandler?.areNotificationsEnabled() == true) {
                                platformActionHandler?.playNotificationSound()
                            }
                            platformActionHandler?.showNewsNotification(newUniqueItems.map { it.title }, newUniqueItems.size)
                        }
                    }
                    saveSettings(_appSettings.value.copy(lastSync = now))
                    return@withContext newUniqueItems.size
                } else {
                    // Fetch succeeded but no new items were found.
                    // We MUST update lastSync here so the 5-minute skip rule applies, preventing
                    // the backup alarm from immediately repeating the same network request after WorkManager.
                    saveSettings(_appSettings.value.copy(lastSync = now))
                    _lastFetchedCount.value = 0
                    return@withContext 0
                }
            } else {
                // Feeds might be empty initially, still counts as a successful connection check
                saveSettings(_appSettings.value.copy(lastSync = now))
            }
            return@withContext 0
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            e.printStackTrace()
            _lastFetchedCount.value = -2 // signal error
            return@withContext -2
        } finally {
            _syncing.value = false
        }
    }

    fun deleteNewsItem(id: String) {
        // Remove from UI list
        val updated = _newsList.value.filter { it.id != id }
        saveLocalNews(updated)
    }

    fun clearNewsCache() {
        saveLocalNews(emptyList())
        settings.putString("Top_Link", "")
        settings.putString("Top_Time", "")
        settings.putString("Bottom_Link", "")
        settings.putString("Bottom_Time", "")
        settings.putString("unnotified_fcm_titles", "")
        _lastFetchedCount.value = -1
    }

    fun restoreNewsItem(item: NewsItem, index: Int) {
        val current = _newsList.value.toMutableList()
        if (index in 0..current.size) {
            current.add(index, item)
        } else {
            current.add(item)
        }
        saveLocalNews(current)
    }
}

object ClockSystem {
    fun currentTimeMillis(): Long {
        // Native currentTimeMillis is equivalent across Kotlin/JS (Date.now()) and Kotlin/JVM (System.currentTimeMillis())
        // but we write a safe cross-platform wrapper using system-specific or epoch wrappers.
        // In Kotlin commonMain, we can get current time using standard expect/actual or System class if supported,
        // but Kotlin's Clock or simple platform wrapper is best.
        // Actually, we can use a small actual function for System.currentTimeMillis() or Wasm Date.now()!
        return PlatformClock.currentTimeMillis()
    }
}

expect object PlatformClock {
    fun currentTimeMillis(): Long
}
