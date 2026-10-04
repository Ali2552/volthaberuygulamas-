package com.example.data.remote

import android.util.Log
import com.example.data.config.SourceConfig
import com.example.data.model.Article
import com.example.data.model.FetchMethod
import com.example.data.model.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.pow

object DataFetcher {
    private const val TAG = "DataFetcher"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(SourceConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(SourceConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(SourceConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Executes HTTP GET with exponential backoff and User-Agent rotation on 403/429/failures.
     */
    suspend fun executeGetWithRetry(url: String): Pair<String, Int> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        var lastStatusCode = 0

        for (attempt in 0 until SourceConfig.MAX_RETRIES) {
            val userAgent = SourceConfig.USER_AGENTS[attempt % SourceConfig.USER_AGENTS.size]
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)

                SourceConfig.DEFAULT_HEADERS.forEach { (key, value) ->
                    requestBuilder.header(key, value)
                }

                val response: Response = client.newCall(requestBuilder.build()).execute()
                lastStatusCode = response.code

                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    response.close()
                    return@withContext Pair(bodyString, lastStatusCode)
                }

                // If 429 or 403 or server error, apply exponential backoff
                response.close()
                if (response.code == 429 || response.code == 403 || response.code >= 500) {
                    val backoffTime = (SourceConfig.RETRY_BACKOFF_BASE_MS * 2.0.pow(attempt.toDouble())).toLong()
                    Log.w(TAG, "Rate limited or blocked (${response.code}) on $url. Attempt ${attempt + 1}, backing off ${backoffTime}ms with UA rotation.")
                    delay(backoffTime)
                } else {
                    // Other non-retryable 4xx
                    break
                }
            } catch (e: IOException) {
                lastException = e
                val backoffTime = (SourceConfig.RETRY_BACKOFF_BASE_MS * 2.0.pow(attempt.toDouble())).toLong()
                Log.w(TAG, "IOException on $url (attempt ${attempt + 1}): ${e.message}, waiting ${backoffTime}ms")
                delay(backoffTime)
            } catch (e: Exception) {
                lastException = e
                break
            }
        }
        if (lastException != null) {
            throw lastException
        }
        throw IOException("HTTP request failed with status code $lastStatusCode for $url")
    }

    /**
     * Fetches HTML directly via Jsoup with retry and UA rotation.
     */
    suspend fun fetchDocument(url: String): Document = withContext(Dispatchers.IO) {
        val (html, _) = executeGetWithRetry(url)
        Jsoup.parse(html, url)
    }

    /**
     * Auto-discovers RSS URL from HTML document (<link rel="alternate" type="application/rss+xml">)
     */
    suspend fun discoverRssUrl(baseUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = fetchDocument(baseUrl)
            val link = doc.select("link[type*=\"rss\"], link[type*=\"atom\"], link[rel*=\"alternate\"][type*=\"xml\"]").first()
            val href = link?.attr("abs:href")
            if (!href.isNullOrBlank()) {
                return@withContext href
            }

            // Common default paths to check
            val commonPaths = listOf("/rss", "/feed", "/rss.xml", "/feed.xml", "/rss/")
            for (path in commonPaths) {
                val fullUrl = if (baseUrl.endsWith("/")) "${baseUrl.removeSuffix("/")}$path" else "$baseUrl$path"
                try {
                    val (content, code) = executeGetWithRetry(fullUrl)
                    if (code == 200 && (content.contains("<rss") || content.contains("<feed"))) {
                        return@withContext fullUrl
                    }
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto RSS discovery failed for $baseUrl: ${e.message}")
        }
        null
    }

    /**
     * Resilient Article Fetcher:
     * 1. Try Primary RSS
     * 2. Try Fallback RSS list
     * 3. Try Auto-discovered RSS
     * 4. Fallback to HTML scraping
     */
    suspend fun fetchArticlesResilient(
        sourceId: String,
        sourceName: String,
        baseUrl: String,
        primaryRss: String?,
        fallbackRssList: List<String> = emptyList(),
        htmlSelector: String = "",
        titleSelector: String = "h2, h3, a",
        isEnglish: Boolean = false,
        onHtmlParse: ((Document) -> List<Article>)? = null
    ): FetchResult<List<Article>> = withContext(Dispatchers.IO) {
        // Step 1: Try Primary RSS
        if (!primaryRss.isNullOrBlank()) {
            try {
                val (xml, _) = executeGetWithRetry(primaryRss)
                val articles = RssAtomParser.parse(xml, sourceId, sourceName, isEnglish)
                if (articles.isNotEmpty()) {
                    return@withContext FetchResult(articles, FetchMethod.RSS)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Primary RSS failed for $sourceName ($primaryRss): ${e.message}")
            }
        }

        // Step 2: Try Fallback RSS List
        for (rssUrl in fallbackRssList) {
            try {
                val (xml, _) = executeGetWithRetry(rssUrl)
                val articles = RssAtomParser.parse(xml, sourceId, sourceName, isEnglish)
                if (articles.isNotEmpty()) {
                    return@withContext FetchResult(articles, FetchMethod.RSS)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Fallback RSS failed for $sourceName ($rssUrl): ${e.message}")
            }
        }

        // Step 3: Try Auto-discovering RSS from HTML
        try {
            val discoveredRss = discoverRssUrl(baseUrl)
            if (!discoveredRss.isNullOrBlank() && discoveredRss != primaryRss) {
                val (xml, _) = executeGetWithRetry(discoveredRss)
                val articles = RssAtomParser.parse(xml, sourceId, sourceName, isEnglish)
                if (articles.isNotEmpty()) {
                    return@withContext FetchResult(articles, FetchMethod.RSS)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Discovered RSS failed for $sourceName: ${e.message}")
        }

        // Step 4: Fallback to HTML Scraping with Jsoup
        try {
            val doc = fetchDocument(baseUrl)
            val articles = if (onHtmlParse != null) {
                onHtmlParse(doc)
            } else {
                scrapeArticlesFromHtml(doc, sourceId, sourceName, baseUrl, htmlSelector, titleSelector, isEnglish)
            }

            if (articles.isNotEmpty()) {
                return@withContext FetchResult(articles, FetchMethod.HTML)
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTML scraping failed for $sourceName: ${e.message}")
            return@withContext FetchResult(
                data = emptyList(),
                method = FetchMethod.HTML,
                errorMessage = e.localizedMessage ?: "Veri çekilemedi"
            )
        }

        FetchResult(
            data = emptyList(),
            method = FetchMethod.HTML,
            errorMessage = "İçerik bulunamadı"
        )
    }

    private fun scrapeArticlesFromHtml(
        doc: Document,
        sourceId: String,
        sourceName: String,
        baseUrl: String,
        itemSelector: String,
        titleSelector: String,
        isEnglish: Boolean
    ): List<Article> {
        val list = mutableListOf<Article>()
        val elements = if (itemSelector.isNotBlank()) doc.select(itemSelector) else doc.select("article, .news-item")

        for (el in elements) {
            val titleEl = el.select(titleSelector).first() ?: el.select("a, h1, h2, h3, h4").first()
            val title = titleEl?.text()?.trim() ?: ""
            var link = el.select("a").first()?.attr("abs:href") ?: ""
            if (link.isBlank() && el.`is`("a")) {
                link = el.attr("abs:href")
            }
            val img = el.select("img").first()?.let {
                val src = it.attr("abs:src")
                if (src.isBlank()) it.attr("data-src") else src
            }
            val desc = el.select("p, .desc, .summary").first()?.text() ?: ""

            if (title.isNotBlank() && title.length > 5 && link.isNotBlank()) {
                list.add(
                    Article(
                        url = link,
                        title = title,
                        description = desc,
                        sourceId = sourceId,
                        sourceName = sourceName,
                        publishedDate = "Bugün",
                        timestamp = System.currentTimeMillis(),
                        imageUrl = img?.takeIf { it.isNotBlank() },
                        isEnglish = isEnglish,
                        fetchMethod = FetchMethod.HTML
                    )
                )
            }
            if (list.size >= 35) break
        }
        return list
    }
}
