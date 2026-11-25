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
data class CloudBookEntry(
    val id: String = "",
    val title: String = "",
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
    fun resolvedId(): String {
        if (id.isNotBlank()) return id.trim()
        if (pdf_path.isNotBlank()) {
            val staticMatch = (com.Nightjar.Th3GradeLibraryKMP.data.AllItems.books + com.Nightjar.Th3GradeLibraryKMP.data.AllItems.notes)
                .firstOrNull { it.pdfPath.equals(pdf_path.trim(), ignoreCase = true) }
            if (staticMatch != null) return staticMatch.id
        }
        val fromPath = pdf_path.substringAfterLast("/").substringBeforeLast(".").trim()
        if (fromPath.isNotBlank()) return "auto_$fromPath"
        if (stream_url.isNotBlank()) return "auto_${stream_url.hashCode()}"
        if (title.isNotBlank()) return "auto_${title.hashCode()}"
        return "auto_item"
    }

    fun resolvedTitle(): String {
        if (title.isNotBlank()) return title.trim()
        return if (is_note) "ملزمة دراسية" else "كتاب دراسي"
    }

    fun toBookItem(): BookItem {
        val rId = resolvedId()
        return BookItem(
            id = rId,
            title = resolvedTitle(),
            coverResName = cover_res,
            pdfPath = pdf_path.ifBlank { if (is_note) "files/notes/$rId.pdf" else "files/books/$rId.pdf" },
            isNote = is_note,
            author = author.ifBlank { if (is_note) "ملزمة" else "كتاب" },
            colorStart = color_start.ifBlank { "#2563eb" },
            colorEnd = color_end.ifBlank { "#1d4ed8" }
        )
    }
}

@Serializable
data class CloudManifest(
    val version: Int = 2,
    val grade: String = "الثالث متوسط",
    val hidden_ids: List<String> = emptyList(),
    val books: List<CloudBookEntry> = emptyList(),
    val notes: List<CloudBookEntry> = emptyList()
)

object CloudCatalogManager {
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        encodeDefaults = true
    }
    
    private val httpClient = HttpClient()

    // Dual Edge Mirrors for 100% High Availability (Cloudflare Worker & GitHub Raw / jsDelivr CDN)
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
                if (parsed.books.isNotEmpty() || parsed.notes.isNotEmpty()) {
                    return parsed
                }
            }
        } catch (_: Exception) {}

        return CloudManifest(version = 2)
    }

    private fun updateDynamicLists(m: CloudManifest) {
        if (m.books.isNotEmpty()) {
            _dynamicBooks.value = m.books.map { it.toBookItem() }
        }
        if (m.notes.isNotEmpty()) {
            _dynamicNotes.value = m.notes.map { it.toBookItem() }
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
                            if (parsed.books.isNotEmpty() || parsed.notes.isNotEmpty()) {
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

    fun getEntry(item: BookItem): CloudBookEntry? {
        val current = _manifest.value
        val list = if (item.isNote) current.notes else current.books
        return list.firstOrNull { it.resolvedId() == item.id }
    }

    // Full backwards-compatibility alias for UI and DownloadManager
    fun getMeta(item: BookItem): CloudBookEntry? = getEntry(item)

    /**
     * Direct link generator for external and cloud drives (e.g. Google Drive, Archive, CDNs).
     */
    fun convertToDirectUrl(url: String): String {
        if (url.isBlank()) return ""
        val trimmed = url.trim()
        if (trimmed.contains("drive.google.com") && trimmed.contains("/d/")) {
            val fileId = trimmed.substringAfter("/d/").substringBefore("/").substringBefore("?")
            if (fileId.isNotBlank()) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
            }
        }
        if ((trimmed.contains("drive.google.com") || trimmed.contains("drive.usercontent.google.com")) && trimmed.contains("id=")) {
            val fileId = trimmed.substringAfter("id=").substringBefore("&")
            if (fileId.isNotBlank()) {
                return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t"
            }
        }
        return trimmed
    }

    fun getAllCatalogItems(): List<BookItem> {
        val staticItems = com.Nightjar.Th3GradeLibraryKMP.data.AllItems.books + com.Nightjar.Th3GradeLibraryKMP.data.AllItems.notes
        val dynamicItems = _dynamicBooks.value + _dynamicNotes.value
        return (staticItems + dynamicItems).distinctBy { it.id }
    }

    /**
     * Resolves all available candidate stream/download URLs in priority order:
     * 1. Explicit stream_url / backup_hf_url
     * 2. Custom mirrors (backup_url_3, backup_url_4, backup_github_url)
     * 3. Fallback Hugging Face dataset mirror
     */
    fun getStreamCandidateUrls(item: BookItem): List<String> {
        val entry = getEntry(item)
        val candidates = mutableListOf<String>()
        val filename = entry?.pdf_path?.substringAfterLast("/")?.takeIf { it.isNotBlank() }
            ?: item.pdfPath.substringAfterLast("/").takeIf { it.isNotBlank() }
            ?: "${item.id}.pdf"
        val folder = if (item.isNote) "notes" else "books"

        val hfBackup = entry?.backup_hf_url
        if (!hfBackup.isNullOrBlank()) {
            candidates.add(convertToDirectUrl(hfBackup))
        }

        val primary = entry?.stream_url
        if (!primary.isNullOrBlank()) {
            candidates.add(convertToDirectUrl(primary))
        }

        val b3 = entry?.backup_url_3
        if (!b3.isNullOrBlank()) candidates.add(convertToDirectUrl(b3))

        val b4 = entry?.backup_url_4
        if (!b4.isNullOrBlank()) candidates.add(convertToDirectUrl(b4))

        val githubBackup = entry?.backup_github_url
        if (!githubBackup.isNullOrBlank()) {
            candidates.add(convertToDirectUrl(githubBackup))
        }

        // Last-resort fallback: Hugging Face dataset mirror
        candidates.add("https://huggingface.co/datasets/Larber/th3grade-books/resolve/main/$folder/$filename")

        return candidates.distinct().filter { it.isNotBlank() && it.startsWith("http") }
    }

    fun getStreamUrl(item: BookItem): String {
        return getStreamCandidateUrls(item).firstOrNull() ?: ""
    }
}