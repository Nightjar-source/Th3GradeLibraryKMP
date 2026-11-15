package com.Nightjar.Th3GradeLibraryKMP.network

import com.Nightjar.Th3GradeLibraryKMP.data.BookItem
import com.Nightjar.Th3GradeLibraryKMP.data.AppDataStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DynamicBookItem(
    val id: String,
    val title: String,
    val cover_res: String = "",
    val cover_url: String = "",
    val pdf_path: String = "",
    val is_note: Boolean = false,
    val author: String = "",
    val color_start: String = "#2563eb",
    val color_end: String = "#1d4ed8",
    val file_size: Long = 0,
    val stream_url: String = "",
    val backup_hf_url: String = "",
    val backup_github_url: String = "",
    val backup_url_3: String = "",
    val backup_url_4: String = ""
) {
    fun toBookItem(): BookItem {
        return BookItem(
            id = id,
            title = title,
            coverResName = cover_res,
            pdfPath = pdf_path.ifBlank { if (is_note) "files/notes/$id.pdf" else "files/books/$id.pdf" },
            isNote = is_note,
            author = author,
            colorStart = color_start,
            colorEnd = color_end
        )
    }
}

@Serializable
data class CloudBookMeta(
    val fallback_filename: String = "",
    val title: String = "",
    val file_size: Long? = null,
    val stream_url: String? = null,
    val backup_hf_url: String? = null,
    val backup_github_url: String? = null,
    val backup_url_3: String? = null,
    val backup_url_4: String? = null,
    val direct_url: String? = null,
    val cover_url: String? = null
)

@Serializable
data class CloudManifest(
    val version: Int = 1,
    val grade: String = "الثالث متوسط",
    val hidden_ids: List<String> = emptyList(),
    val books: Map<String, CloudBookMeta> = emptyMap(),
    val notes: Map<String, CloudBookMeta> = emptyMap(),
    val books_list: List<DynamicBookItem> = emptyList(),
    val notes_list: List<DynamicBookItem> = emptyList()
)

object CloudCatalogManager {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        encodeDefaults = true
    }
    
    private val httpClient = HttpClient()

    // Dual Edge Mirrors for 100% High Availability (Cloudflare Worker & GitHub Raw / CDN)
    private const val CLOUDFLARE_MANIFEST_URL = 
        "https://th3books-gateway.karrar1iq.workers.dev/manifest"
    private const val GITHUB_MANIFEST_RAW = 
        "https://raw.githubusercontent.com/Nightjar-source/Th3GradeLibraryKMP-Cloud/main/cloud_manifest.json"
    private const val JSDELIVR_MANIFEST_CDN = 
        "https://cdn.jsdelivr.net/gh/Nightjar-source/Th3GradeLibraryKMP-Cloud@main/cloud_manifest.json"

    private const val CACHE_KEY_MANIFEST = "cached_cloud_manifest_json"

    private val _manifest = MutableStateFlow(loadInitialManifest())
    val manifest = _manifest.asStateFlow()

    private val _dynamicBooks = MutableStateFlow<List<BookItem>>(emptyList())
    val dynamicBooks = _dynamicBooks.asStateFlow()

    private val _dynamicNotes = MutableStateFlow<List<BookItem>>(emptyList())
    val dynamicNotes = _dynamicNotes.asStateFlow()

    init {
        updateDynamicLists(_manifest.value)
    }

    private fun loadInitialManifest(): CloudManifest {
        try {
            val cachedJson = AppDataStore.getString(CACHE_KEY_MANIFEST, "")
            if (cachedJson.isNotBlank()) {
                val parsed = json.decodeFromString<CloudManifest>(cachedJson)
                if (parsed.books_list.isNotEmpty() || parsed.books.isNotEmpty()) {
                    return parsed
                }
            }
        } catch (_: Exception) {}

        return CloudManifest(version = 1)
    }

    private fun updateDynamicLists(m: CloudManifest) {
        if (m.books_list.isNotEmpty()) {
            _dynamicBooks.value = m.books_list.map { it.toBookItem() }
        }
        if (m.notes_list.isNotEmpty()) {
            _dynamicNotes.value = m.notes_list.map { it.toBookItem() }
        }
    }

    /**
     * Resilient Multi-Source Synchronization:
     * Attempts Cloudflare Edge first -> falls back to GitHub Raw -> falls back to jsDelivr CDN.
     * Updates locally and persistently without requiring APK updates.
     */
    suspend fun refreshManifest() {
        withContext(Dispatchers.IO) {
            try {
                val urls = listOf(CLOUDFLARE_MANIFEST_URL, GITHUB_MANIFEST_RAW, JSDELIVR_MANIFEST_CDN)
                for (url in urls) {
                    try {
                        val response = httpClient.get(url)
                        val text = response.bodyAsText()
                        if (text.isNotBlank() && text.contains("grade")) {
                            val parsed = json.decodeFromString<CloudManifest>(text)
                            if (parsed.books.isNotEmpty() || parsed.books_list.isNotEmpty()) {
                                _manifest.value = parsed
                                updateDynamicLists(parsed)
                                try {
                                    AppDataStore.putString(CACHE_KEY_MANIFEST, text)
                                } catch (_: Exception) {}
                                return@withContext
                            }
                        }
                    } catch (_: Exception) {
                        // Fallback to next mirror
                    }
                }
            } catch (_: Exception) {
                // Silently preserve existing cached manifest
            }
        }
    }

    fun getMeta(item: BookItem): CloudBookMeta? {
        val current = _manifest.value
        val fromMap = if (item.isNote) current.notes[item.id] else current.books[item.id]
        if (fromMap != null) return fromMap

        val fromList = (current.books_list + current.notes_list).firstOrNull { it.id == item.id }
        if (fromList != null) {
            return CloudBookMeta(
                fallback_filename = fromList.pdf_path.substringAfterLast("/"),
                title = fromList.title,
                file_size = fromList.file_size,
                stream_url = fromList.stream_url,
                backup_hf_url = fromList.backup_hf_url,
                backup_github_url = fromList.backup_github_url,
                backup_url_3 = fromList.backup_url_3,
                backup_url_4 = fromList.backup_url_4,
                direct_url = fromList.stream_url,
                cover_url = fromList.cover_url
            )
        }
        return null
    }

    /**
     * Direct link generator for external and cloud drives (e.g. Google Drive, Archive, CDNs).
     */
    fun convertToDirectUrl(url: String): String {
        if (url.isBlank()) return ""
        if (url.contains("drive.google.com") && url.contains("/d/")) {
            val fileId = url.substringAfter("/d/").substringBefore("/").substringBefore("?")
            if (fileId.isNotBlank()) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download"
            }
        }
        if (url.contains("drive.google.com") && url.contains("id=")) {
            val fileId = url.substringAfter("id=").substringBefore("&")
            if (fileId.isNotBlank()) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download"
            }
        }
        return url
    }

    fun getAllCatalogItems(): List<BookItem> {
        val staticItems = com.Nightjar.Th3GradeLibraryKMP.data.AllItems.books + com.Nightjar.Th3GradeLibraryKMP.data.AllItems.notes
        val dynamicItems = _dynamicBooks.value + _dynamicNotes.value
        val current = _manifest.value
        val mapItems = mutableListOf<BookItem>()

        current.books.forEach { (id, meta) ->
            if (staticItems.none { it.id == id } && dynamicItems.none { it.id == id }) {
                mapItems.add(
                    BookItem(
                        id = id,
                        title = meta.title.ifBlank { id },
                        coverResName = "",
                        pdfPath = "files/books/$id.pdf",
                        isNote = false,
                        author = "وزارة التربية العراقية",
                        colorStart = "#1E293B",
                        colorEnd = "#0F172A"
                    )
                )
            }
        }

        current.notes.forEach { (id, meta) ->
            if (staticItems.none { it.id == id } && dynamicItems.none { it.id == id }) {
                mapItems.add(
                    BookItem(
                        id = id,
                        title = meta.title.ifBlank { id },
                        coverResName = "",
                        pdfPath = "files/notes/$id.pdf",
                        isNote = true,
                        author = "ملازم الثالث متوسط",
                        colorStart = "#1E293B",
                        colorEnd = "#0F172A"
                    )
                )
            }
        }

        return (staticItems + dynamicItems + mapItems).distinctBy { it.id }
    }

    /**
     * Resolves all available candidate stream/download URLs in priority order:
     * 1. Primary: Official Ministry of Education / Declared stream_url
     * 2. Secondary: Hugging Face mirror (backup_hf_url)
     * 3. Tertiary: GitHub Releases direct mirror (backup_github_url)
     * 4. Multi-tier: Any custom declared 3rd / 4th direct mirrors
     */
    fun getStreamCandidateUrls(item: BookItem): List<String> {
        val meta = getMeta(item)
        val candidates = mutableListOf<String>()

        // 1. Primary: Official Ministry or stream_url / direct_url
        val primary = meta?.stream_url ?: meta?.direct_url
        if (!primary.isNullOrBlank()) {
            candidates.add(convertToDirectUrl(primary))
        }

        // 2. Secondary: Hugging Face dataset mirror
        val hfBackup = meta?.backup_hf_url
        if (!hfBackup.isNullOrBlank()) {
            candidates.add(convertToDirectUrl(hfBackup))
        } else {
            val filename = meta?.fallback_filename
            if (!filename.isNullOrBlank()) {
                val folder = if (item.isNote) "notes" else "books"
                candidates.add("https://huggingface.co/datasets/Larber/th3grade-books/resolve/main/$folder/$filename")
            }
        }

        // 3. Tertiary: GitHub Releases direct backup
        val githubBackup = meta?.backup_github_url
        if (!githubBackup.isNullOrBlank()) {
            candidates.add(convertToDirectUrl(githubBackup))
        } else {
            val filename = meta?.fallback_filename
            if (!filename.isNullOrBlank()) {
                candidates.add("https://github.com/Nightjar-source/Th3GradeLibraryKMP-Cloud/releases/download/v2.0.0-cloud/$filename")
            }
        }

        // 4. Custom 3rd / 4th tier backups (Google Drive, Archive, Custom CDN)
        val b3 = meta?.backup_url_3
        if (!b3.isNullOrBlank()) candidates.add(convertToDirectUrl(b3))

        val b4 = meta?.backup_url_4
        if (!b4.isNullOrBlank()) candidates.add(convertToDirectUrl(b4))

        return candidates.distinct().filter { it.isNotBlank() && it.startsWith("http") }
    }

    fun getStreamUrl(item: BookItem): String {
        return getStreamCandidateUrls(item).firstOrNull() ?: ""
    }
}