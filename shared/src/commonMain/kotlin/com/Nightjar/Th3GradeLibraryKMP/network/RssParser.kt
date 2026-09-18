package com.Nightjar.Th3GradeLibraryKMP.network

import com.Nightjar.Th3GradeLibraryKMP.data.NewsItem

object RssParser {
    fun parse(xml: String): List<NewsItem> {
        val items = mutableListOf<NewsItem>()
        var searchIndex = 0
        while (true) {
            val itemStart = xml.indexOf("<item>", searchIndex)
            if (itemStart == -1) break
            val itemEnd = xml.indexOf("</item>", itemStart)
            if (itemEnd == -1) break
            
            val itemXml = xml.substring(itemStart + 6, itemEnd)
            
            val title = decodeHtmlEntities(extractTag(itemXml, "title") ?: "")
            val link = extractTag(itemXml, "link") ?: ""
            val pubDate = extractTag(itemXml, "pubDate") ?: ""
            val contentSnippet = decodeHtmlEntities(extractTag(itemXml, "description") 
                ?.replace(Regex("<[^>]*>"), "") // strip HTML tags
                ?.replace("\n", " ")
                ?.replace(Regex("\\s+"), " ")
                ?.trim() ?: "")
            
            // Extract image url
            // Sometimes it's in enclosure or media:thumbnail url
            var imageUrl: String? = null
            
            val enclosureStart = itemXml.indexOf("<enclosure")
            if (enclosureStart != -1) {
                val urlIndex = itemXml.indexOf("url=\"", enclosureStart)
                if (urlIndex != -1 && urlIndex < itemXml.indexOf(">", enclosureStart)) {
                    val urlEnd = itemXml.indexOf("\"", urlIndex + 5)
                    if (urlEnd != -1) {
                        imageUrl = itemXml.substring(urlIndex + 5, urlEnd)
                    }
                }
            }
            if (imageUrl == null) {
                val mediaContentStart = itemXml.indexOf("media:content")
                if (mediaContentStart != -1) {
                    val urlIndex = itemXml.indexOf("url=\"", mediaContentStart)
                    if (urlIndex != -1) {
                        val urlEnd = itemXml.indexOf("\"", urlIndex + 5)
                        imageUrl = itemXml.substring(urlIndex + 5, urlEnd)
                    }
                }
            }
            if (imageUrl == null) {
                val mediaThumbnailStart = itemXml.indexOf("media:thumbnail")
                if (mediaThumbnailStart != -1) {
                    val urlIndex = itemXml.indexOf("url=\"", mediaThumbnailStart)
                    if (urlIndex != -1) {
                        val urlEnd = itemXml.indexOf("\"", urlIndex + 5)
                        imageUrl = itemXml.substring(urlIndex + 5, urlEnd)
                    }
                }
            }
            if (imageUrl == null) {
                val desc = extractTag(itemXml, "description") ?: ""
                val match = Regex("""<img[^>]+src=["']([^"']+)["']""").find(desc)
                if (match != null) {
                    imageUrl = match.groupValues[1]
                }
            }
            
            // تحويل الصور المصغرة القادمة من Blogger إلى دقة أصلية عالية الجودة
            if (imageUrl != null) {
                imageUrl = imageUrl!!.replace(Regex("/s\\d+(-c)?/"), "/s0/")
                imageUrl = imageUrl!!.replace("/w72-h72-p-k-no-nu/", "/s0/")
            }
            
            items.add(NewsItem(
                id = link.ifEmpty { title },
                title = title,
                link = link,
                contentSnippet = contentSnippet,
                pubDate = pubDate,
                imageUrl = imageUrl
            ))
            
            searchIndex = itemEnd + 7
        }
        return items
    }

    private fun extractTag(xml: String, tagName: String): String? {
        val startTag = "<$tagName>"
        val endTag = "</$tagName>"
        val start = xml.indexOf(startTag)
        if (start == -1) {
            // Check for tags with namespaces or attributes e.g. <link rel="...">
            val altStart = xml.indexOf("<$tagName ")
            if (altStart != -1) {
                val tagEndClose = xml.indexOf(">", altStart)
                val end = xml.indexOf(endTag, tagEndClose)
                if (tagEndClose != -1 && end != -1) {
                    return xml.substring(tagEndClose + 1, end).trim()
                }
            }
            return null
        }
        val end = xml.indexOf(endTag, start)
        if (end == -1) return null
        
        var content = xml.substring(start + startTag.length, end).trim()
        if (content.startsWith("<![CDATA[") && content.endsWith("]]>")) {
            content = content.substring(9, content.length - 3).trim()
        }
        return content
    }

    private fun decodeHtmlEntities(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&#039;", "'")
            .replace("&nbsp;", " ")
    }
}
