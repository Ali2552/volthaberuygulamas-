package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.example.data.config.SourceConfig
import com.example.data.model.PriceSearchResult
import com.example.data.model.StoreDiagnosticInfo
import com.example.data.model.StoreStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URLEncoder
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object PriceSearchFetcher {
    private const val TAG = "PriceSearchFetcher"

    // Circuit Breaker: storeId -> blockedUntilTimestamp
    private val circuitBreakers = ConcurrentHashMap<String, Long>()

    // Diagnostics store: storeId -> StoreDiagnosticInfo
    val diagnosticMap = ConcurrentHashMap<String, StoreDiagnosticInfo>()

    // Disabled stores set (user can toggle off)
    val disabledStores = ConcurrentHashMap.newKeySet<String>()

    private val turkishCurrencyFormat = DecimalFormat("#,##0.00 TL", DecimalFormatSymbols(Locale("tr", "TR")))

    suspend fun searchStore(
        store: SourceConfig.StoreConfig,
        query: String
    ): List<PriceSearchResult> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        if (disabledStores.contains(store.id)) {
            diagnosticMap[store.id] = StoreDiagnosticInfo(
                storeId = store.id,
                storeName = store.displayName,
                layer = store.layer,
                status = StoreStatus.DISABLED,
                isEnabled = false
            )
            return@withContext emptyList()
        }

        // Check Circuit Breaker
        val blockedUntil = circuitBreakers[store.id] ?: 0L
        if (now < blockedUntil) {
            diagnosticMap[store.id] = StoreDiagnosticInfo(
                storeId = store.id,
                storeName = store.displayName,
                layer = store.layer,
                status = StoreStatus.BLOCKED,
                lastSuccessTime = diagnosticMap[store.id]?.lastSuccessTime,
                httpCode = 429,
                requestUrl = "Devre kesici aktif (10 dk mola)"
            )
            return@withContext emptyList()
        }

        // Jitter (300 - 900 ms)
        delay(Random.nextLong(300, 900))

        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = store.searchUrlTemplate.replace("{query}", encodedQuery)

        var httpStatusCode = 200
        var rawHtml = ""

        try {
            val responsePair = DataFetcher.executeGetWithRetry(url)
            rawHtml = responsePair.first
            httpStatusCode = responsePair.second

            if (rawHtml.isBlank() || rawHtml.contains("Cloudflare") || rawHtml.contains("cf-browser-verification") || rawHtml.contains("captcha")) {
                circuitBreakers[store.id] = now + SourceConfig.CIRCUIT_BREAKER_DURATION_MS
                diagnosticMap[store.id] = StoreDiagnosticInfo(
                    storeId = store.id,
                    storeName = store.displayName,
                    layer = store.layer,
                    status = StoreStatus.BLOCKED,
                    httpCode = if (httpStatusCode == 200) 403 else httpStatusCode,
                    requestUrl = url,
                    rawHtmlSnippet = rawHtml.take(15000)
                )
                return@withContext emptyList()
            }

            val doc = Jsoup.parse(rawHtml, url)

            // Step 1: Try JSON-LD or Embedded Script JSON
            val jsonLdResults = extractFromJsonLd(doc, store, query)
            if (jsonLdResults.isNotEmpty()) {
                recordSuccess(store, jsonLdResults.size, httpStatusCode, url)
                return@withContext jsonLdResults
            }

            // Step 2: Try HTML Selectors (with fallback list)
            val failedSelectors = mutableListOf<String>()
            val htmlResults = extractFromHtml(doc, store, query, failedSelectors)

            if (htmlResults.isNotEmpty()) {
                recordSuccess(store, htmlResults.size, httpStatusCode, url)
                return@withContext htmlResults
            } else {
                // Selector broken
                diagnosticMap[store.id] = StoreDiagnosticInfo(
                    storeId = store.id,
                    storeName = store.displayName,
                    layer = store.layer,
                    status = StoreStatus.SELECTOR_BROKEN,
                    httpCode = httpStatusCode,
                    requestUrl = url,
                    failedSelectors = failedSelectors,
                    rawHtmlSnippet = rawHtml.take(15000)
                )
                return@withContext emptyList()
            }
        } catch (e: IOException) {
            val isBlocked = e.message?.contains("403") == true || e.message?.contains("429") == true
            if (isBlocked) {
                circuitBreakers[store.id] = now + SourceConfig.CIRCUIT_BREAKER_DURATION_MS
            }
            diagnosticMap[store.id] = StoreDiagnosticInfo(
                storeId = store.id,
                storeName = store.displayName,
                layer = store.layer,
                status = if (isBlocked) StoreStatus.BLOCKED else StoreStatus.TIMEOUT,
                httpCode = if (isBlocked) 403 else 504,
                requestUrl = url,
                rawHtmlSnippet = e.localizedMessage ?: "Bağlantı hatası"
            )
            return@withContext emptyList()
        } catch (e: Exception) {
            diagnosticMap[store.id] = StoreDiagnosticInfo(
                storeId = store.id,
                storeName = store.displayName,
                layer = store.layer,
                status = StoreStatus.SELECTOR_BROKEN,
                httpCode = 500,
                requestUrl = url,
                rawHtmlSnippet = e.localizedMessage ?: ""
            )
            return@withContext emptyList()
        }
    }

    private fun extractFromJsonLd(
        doc: Document,
        store: SourceConfig.StoreConfig,
        query: String
    ): List<PriceSearchResult> {
        val results = mutableListOf<PriceSearchResult>()
        try {
            val scripts = doc.select("script[type='application/ld+json']")
            for (script in scripts) {
                val data = script.data().trim()
                if (data.isBlank() || (!data.contains("Product") && !data.contains("offers"))) continue

                if (data.startsWith("[")) {
                    val arr = JSONArray(data)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        parseJsonLdProduct(obj, store, query)?.let { results.add(it) }
                    }
                } else if (data.startsWith("{")) {
                    val obj = JSONObject(data)
                    if (obj.optString("@type") == "ItemList") {
                        val elements = obj.optJSONArray("itemListElement") ?: continue
                        for (i in 0 until elements.length()) {
                            val itemObj = elements.optJSONObject(i)?.optJSONObject("item") ?: continue
                            parseJsonLdProduct(itemObj, store, query)?.let { results.add(it) }
                        }
                    } else {
                        parseJsonLdProduct(obj, store, query)?.let { results.add(it) }
                    }
                }
            }
        } catch (_: Exception) {}
        return results
    }

    private fun parseJsonLdProduct(
        obj: JSONObject,
        store: SourceConfig.StoreConfig,
        query: String
    ): PriceSearchResult? {
        val name = obj.optString("name", "")
        if (name.isBlank() || !isRelevant(name, query)) return null

        val offers = obj.optJSONObject("offers") ?: obj.optJSONArray("offers")?.optJSONObject(0)
        val price = offers?.optDouble("price", 0.0) ?: 0.0
        if (price <= 0.0) return null

        val url = obj.optString("url", offers?.optString("url", store.searchUrlTemplate))
        val image = obj.optString("image", "")

        return PriceSearchResult(
            id = "${store.id}_${name.hashCode()}",
            title = name.trim(),
            storeId = store.id,
            storeName = store.displayName,
            price = price,
            priceFormatted = turkishCurrencyFormat.format(price),
            productUrl = url,
            imageUrl = image.takeIf { it.isNotBlank() },
            appPackageName = store.appPackageName,
            layer = store.layer,
            relevanceScore = calculateRelevance(name, query)
        )
    }

    private fun extractFromHtml(
        doc: Document,
        store: SourceConfig.StoreConfig,
        query: String,
        failedSelectors: MutableList<String>
    ): List<PriceSearchResult> {
        val results = mutableListOf<PriceSearchResult>()

        var cardElements = org.jsoup.select.Elements()
        for (itemSel in store.itemSelectors) {
            val found = doc.select(itemSel)
            if (found.isNotEmpty()) {
                cardElements = found
                break
            } else {
                failedSelectors.add(itemSel)
            }
        }

        if (cardElements.isEmpty()) return emptyList()

        for (card in cardElements) {
            val title = extractTextBySelectors(card, store.titleSelectors)
            val priceStr = extractTextBySelectors(card, store.priceSelectors)
            val link = extractAttrBySelectors(card, store.linkSelectors, "abs:href")
            val image = extractAttrBySelectors(card, store.imageSelectors, "src")
                .ifBlank { extractAttrBySelectors(card, store.imageSelectors, "data-src") }

            // Sub-store name for aggregators (Cimri, Akakçe)
            val subStore = if (store.storeNameSelectors.isNotEmpty()) {
                extractTextBySelectors(card, store.storeNameSelectors).ifBlank { store.displayName }
            } else {
                store.displayName
            }

            val price = cleanPrice(priceStr)

            if (title.isNotBlank() && price > 0.0 && isRelevant(title, query)) {
                results.add(
                    PriceSearchResult(
                        id = "${store.id}_${title.hashCode()}_${results.size}",
                        title = title.trim(),
                        storeId = store.id,
                        storeName = subStore.trim(),
                        price = price,
                        priceFormatted = turkishCurrencyFormat.format(price),
                        productUrl = link.ifBlank { store.searchUrlTemplate.replace("{query}", query) },
                        imageUrl = image.takeIf { it.isNotBlank() },
                        appPackageName = store.appPackageName,
                        layer = store.layer,
                        relevanceScore = calculateRelevance(title, query)
                    )
                )
            }
        }

        return results
    }

    private fun extractTextBySelectors(element: org.jsoup.nodes.Element, selectors: List<String>): String {
        for (sel in selectors) {
            val el = element.select(sel).first()
            if (el != null && el.text().isNotBlank()) {
                return el.text().trim()
            }
        }
        return ""
    }

    private fun extractAttrBySelectors(element: org.jsoup.nodes.Element, selectors: List<String>, attr: String): String {
        for (sel in selectors) {
            val el = element.select(sel).first()
            if (el != null) {
                val value = el.attr(attr)
                if (value.isNotBlank()) return value
            }
        }
        return ""
    }

    private fun isRelevant(title: String, query: String): Boolean {
        val tLower = title.lowercase(Locale("tr", "TR"))
        val qWords = query.lowercase(Locale("tr", "TR"))
            .split(" ")
            .filter { it.length > 2 }

        if (qWords.isEmpty()) return true

        // Match at least half of the words
        val matchCount = qWords.count { tLower.contains(it) }
        return matchCount >= (qWords.size / 2).coerceAtLeast(1)
    }

    private fun calculateRelevance(title: String, query: String): Int {
        val tLower = title.lowercase(Locale("tr", "TR"))
        val qWords = query.lowercase(Locale("tr", "TR")).split(" ").filter { it.isNotBlank() }

        var score = 100
        for (w in qWords) {
            if (tLower.contains(w)) score += 20
        }

        // Penalize accessories / parts
        for (excluded in SourceConfig.PriceSearch.EXCLUDED_ACC_KEYWORDS) {
            if (tLower.contains(excluded) && !query.lowercase(Locale("tr", "TR")).contains(excluded)) {
                score -= 80
            }
        }
        return score
    }

    private fun cleanPrice(str: String): Double {
        if (str.isBlank()) return 0.0
        val sanitized = str.replace(".", "")
            .replace(",", ".")
            .replace("TL", "")
            .replace("₺", "")
            .replace("USD", "")
            .trim()
        val match = Regex("\\d+\\.?\\d*").find(sanitized)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }

    private fun recordSuccess(store: SourceConfig.StoreConfig, count: Int, httpCode: Int, url: String) {
        val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        diagnosticMap[store.id] = StoreDiagnosticInfo(
            storeId = store.id,
            storeName = store.displayName,
            layer = store.layer,
            status = StoreStatus.SUCCESS,
            lastSuccessTime = timeNow,
            foundCount = count,
            httpCode = httpCode,
            requestUrl = url
        )
    }
}
