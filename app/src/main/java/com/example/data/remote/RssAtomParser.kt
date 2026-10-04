package com.example.data.remote

import android.util.Xml
import com.example.data.model.Article
import com.example.data.model.FetchMethod
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

object RssAtomParser {

    fun parse(
        xmlContent: String,
        sourceId: String,
        sourceName: String,
        isEnglish: Boolean = false
    ): List<Article> {
        val articles = mutableListOf<Article>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var isAtom = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name?.lowercase(Locale.ROOT)
                if (eventType == XmlPullParser.START_TAG) {
                    if (name == "feed") {
                        isAtom = true
                    } else if (name == "item" || (isAtom && name == "entry")) {
                        val article = if (isAtom) {
                            readAtomEntry(parser, sourceId, sourceName, isEnglish)
                        } else {
                            readRssItem(parser, sourceId, sourceName, isEnglish)
                        }
                        if (article != null && article.title.isNotBlank()) {
                            articles.add(article)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Fallback to Jsoup XML parsing if XmlPullParser fails on malformed XML
            return parseWithJsoupXml(xmlContent, sourceId, sourceName, isEnglish)
        }
        return if (articles.isNotEmpty()) articles else parseWithJsoupXml(xmlContent, sourceId, sourceName, isEnglish)
    }

    private fun readRssItem(
        parser: XmlPullParser,
        sourceId: String,
        sourceName: String,
        isEnglish: Boolean
    ): Article? {
        var title = ""
        var link = ""
        var description = ""
        var pubDate = ""
        var imageUrl: String? = null

        while (parser.next() != XmlPullParser.END_TAG || parser.name?.lowercase(Locale.ROOT) != "item") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            val tagName = parser.name?.lowercase(Locale.ROOT) ?: ""

            when (tagName) {
                "title" -> title = readText(parser)
                "link" -> link = readText(parser)
                "description" -> {
                    val rawDesc = readText(parser)
                    description = cleanHtml(rawDesc)
                    if (imageUrl == null) {
                        imageUrl = extractImageFromHtml(rawDesc)
                    }
                }
                "pubdate" -> pubDate = readText(parser)
                "enclosure" -> {
                    val type = parser.getAttributeValue(null, "type") ?: ""
                    val url = parser.getAttributeValue(null, "url")
                    if (type.startsWith("image") || url?.contains(Regex("\\.(jpg|jpeg|png|webp)", RegexOption.IGNORE_CASE)) == true) {
                        imageUrl = url
                    }
                    skip(parser)
                }
                "media:content", "content" -> {
                    val url = parser.getAttributeValue(null, "url")
                    if (!url.isNullOrBlank()) {
                        imageUrl = url
                    }
                    skip(parser)
                }
                "media:thumbnail" -> {
                    val url = parser.getAttributeValue(null, "url")
                    if (!url.isNullOrBlank()) {
                        imageUrl = url
                    }
                    skip(parser)
                }
                else -> skip(parser)
            }
        }

        if (title.isBlank() && link.isBlank()) return null

        val timestamp = parsePubDateToMillis(pubDate)
        return Article(
            url = link.trim(),
            title = cleanHtml(title).trim(),
            description = description.trim(),
            sourceId = sourceId,
            sourceName = sourceName,
            publishedDate = formatPubDate(pubDate, timestamp),
            timestamp = timestamp,
            imageUrl = imageUrl?.trim(),
            isEnglish = isEnglish,
            fetchMethod = FetchMethod.RSS
        )
    }

    private fun readAtomEntry(
        parser: XmlPullParser,
        sourceId: String,
        sourceName: String,
        isEnglish: Boolean
    ): Article? {
        var title = ""
        var link = ""
        var summary = ""
        var updated = ""
        var imageUrl: String? = null

        while (parser.next() != XmlPullParser.END_TAG || parser.name?.lowercase(Locale.ROOT) != "entry") {
            if (parser.eventType != XmlPullParser.START_TAG) continue
            val tagName = parser.name?.lowercase(Locale.ROOT) ?: ""

            when (tagName) {
                "title" -> title = readText(parser)
                "link" -> {
                    val href = parser.getAttributeValue(null, "href")
                    val rel = parser.getAttributeValue(null, "rel")
                    if (rel == null || rel == "alternate") {
                        if (!href.isNullOrBlank()) link = href
                    }
                    skip(parser)
                }
                "summary", "content" -> {
                    val rawText = readText(parser)
                    summary = cleanHtml(rawText)
                    if (imageUrl == null) {
                        imageUrl = extractImageFromHtml(rawText)
                    }
                }
                "updated", "published" -> updated = readText(parser)
                else -> skip(parser)
            }
        }

        if (title.isBlank()) return null

        val timestamp = parsePubDateToMillis(updated)
        return Article(
            url = link.trim(),
            title = cleanHtml(title).trim(),
            description = summary.trim(),
            sourceId = sourceId,
            sourceName = sourceName,
            publishedDate = formatPubDate(updated, timestamp),
            timestamp = timestamp,
            imageUrl = imageUrl?.trim(),
            isEnglish = isEnglish,
            fetchMethod = FetchMethod.RSS
        )
    }

    private fun parseWithJsoupXml(
        xmlContent: String,
        sourceId: String,
        sourceName: String,
        isEnglish: Boolean
    ): List<Article> {
        val list = mutableListOf<Article>()
        try {
            val doc = Jsoup.parse(xmlContent, "", org.jsoup.parser.Parser.xmlParser())
            val items = doc.select("item, entry")
            for (elem in items) {
                val title = elem.select("title").first()?.text() ?: ""
                var link = elem.select("link").first()?.text() ?: ""
                if (link.isBlank()) {
                    link = elem.select("link").attr("href")
                }
                val desc = elem.select("description, summary, content").first()?.text() ?: ""
                val pubDate = elem.select("pubDate, published, updated").first()?.text() ?: ""
                var img = elem.select("enclosure[type^=image]").attr("url")
                if (img.isBlank()) {
                    img = elem.select("media\\:content, media\\:thumbnail").attr("url")
                }
                if (img.isBlank()) {
                    img = extractImageFromHtml(desc) ?: ""
                }

                if (title.isNotBlank()) {
                    val timestamp = parsePubDateToMillis(pubDate)
                    list.add(
                        Article(
                            url = link.trim(),
                            title = Jsoup.parse(title).text(),
                            description = cleanHtml(desc),
                            sourceId = sourceId,
                            sourceName = sourceName,
                            publishedDate = formatPubDate(pubDate, timestamp),
                            timestamp = timestamp,
                            imageUrl = if (img.isNotBlank()) img.trim() else null,
                            isEnglish = isEnglish,
                            fetchMethod = FetchMethod.RSS
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            return
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    private fun cleanHtml(html: String): String {
        if (html.isBlank()) return ""
        return try {
            Jsoup.parse(html).text()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "")
        }
    }

    private fun extractImageFromHtml(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            val img = doc.select("img").first()
            img?.attr("src")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePubDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
            SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )
        for (format in formats) {
            try {
                val parsed = format.parse(dateStr.trim())
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return System.currentTimeMillis()
    }

    private fun formatPubDate(rawDate: String, timestamp: Long): String {
        if (rawDate.isNotBlank() && rawDate.length < 30 && !rawDate.contains("T")) {
            return rawDate
        }
        val outFormat = SimpleDateFormat("dd MMM HH:mm", Locale("tr", "TR"))
        return outFormat.format(timestamp)
    }
}
