package com.example.data.repository

import android.content.Context
import com.example.data.config.SourceConfig
import com.example.data.local.AppDatabase
import com.example.data.local.ArticleEntity
import com.example.data.local.DataStoreManager
import com.example.data.local.FavoriteLocationEntity
import com.example.data.local.NoteEntity
import com.example.data.local.PriceSearchCacheEntity
import com.example.data.local.RecentSearchEntity
import com.example.data.local.SavedPriceItemEntity
import com.example.data.model.Article
import com.example.data.model.FetchResult
import com.example.data.model.MgmLocation
import com.example.data.model.PriceSearchResult
import com.example.data.model.StoreDiagnosticInfo
import com.example.data.model.WeatherInfo
import com.example.data.remote.DataFetcher
import com.example.data.remote.ElectricFetcher
import com.example.data.remote.NewsFetcher
import com.example.data.remote.PriceSearchFetcher
import com.example.data.remote.WeatherFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AppRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val articleDao = db.articleDao()
    private val noteDao = db.noteDao()
    private val cacheDao = db.cacheDao()
    private val savedPriceDao = db.savedPriceDao()
    private val favoriteLocationDao = db.favoriteLocationDao()
    private val dataStoreManager = DataStoreManager(context)

    private val newsFetcher = NewsFetcher(articleDao)
    private val electricFetcher = ElectricFetcher(articleDao)
    private val weatherFetcher = WeatherFetcher(cacheDao)

    // DataStore Settings
    val themeModeFlow: Flow<String> = dataStoreManager.themeModeFlow
    val showDebugTagsFlow: Flow<Boolean> = dataStoreManager.showDebugTagsFlow
    val selectedLocationFlow: Flow<MgmLocation> = dataStoreManager.selectedLocationFlow

    suspend fun setThemeMode(mode: String) = dataStoreManager.setThemeMode(mode)
    suspend fun setShowDebugTags(show: Boolean) = dataStoreManager.setShowDebugTags(show)
    suspend fun saveSelectedLocation(location: MgmLocation) = dataStoreManager.saveSelectedLocation(location)

    // Haberler (News)
    suspend fun fetchBbcTurkce(): FetchResult<List<Article>> = newsFetcher.fetchBbcTurkce()
    suspend fun fetchSonDakika(): FetchResult<List<Article>> = newsFetcher.fetchSonDakika()

    fun getSavedArticles(isElectric: Boolean): Flow<List<Article>> {
        return articleDao.getSavedArticles(isElectric).map { entities ->
            entities.map { it.toArticle(isSavedExplicit = true, isOutOfDate = false) }
        }
    }

    suspend fun toggleSaveArticle(article: Article, isElectric: Boolean): Boolean {
        val willBeSaved = !article.isSaved
        val entity = ArticleEntity.fromArticle(article, isElectric = isElectric, isSaved = willBeSaved)
        articleDao.insertArticle(entity)
        return willBeSaved
    }

    suspend fun deleteSavedArticle(url: String) {
        articleDao.updateSavedStatus(url, false)
    }

    // Elektrik
    suspend fun fetchElectricalArticles(): FetchResult<List<Article>> = electricFetcher.fetchAllSources()

    // MGM Hava Durumu
    suspend fun fetchWeather(location: MgmLocation): FetchResult<WeatherInfo> = weatherFetcher.fetchMgmWeather(location)

    fun getFavoriteLocations(): Flow<List<FavoriteLocationEntity>> = favoriteLocationDao.getAllFavorites()

    fun isLocationFavorite(merkezId: Int): Flow<Boolean> = favoriteLocationDao.isFavorite(merkezId)

    suspend fun toggleFavoriteLocation(location: MgmLocation): Boolean {
        val isFav = favoriteLocationDao.isFavorite(location.merkezId).firstOrNull() ?: false
        if (isFav) {
            favoriteLocationDao.deleteFavoriteByMerkezId(location.merkezId)
            return false
        } else {
            val count = favoriteLocationDao.getFavoriteCount()
            if (count < 10) {
                favoriteLocationDao.insertFavorite(
                    FavoriteLocationEntity(
                        il = location.il,
                        ilce = location.ilce,
                        merkezId = location.merkezId,
                        istNo = location.istNo,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
                return true
            }
            return false
        }
    }

    suspend fun removeFavoriteLocation(id: Long) {
        favoriteLocationDao.deleteFavoriteById(id)
    }

    // Not Defteri
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return if (query.isBlank()) {
            noteDao.getAllNotes()
        } else {
            noteDao.searchNotes(query.trim())
        }
    }

    suspend fun saveNote(id: Long, title: String, content: String): Long {
        val now = System.currentTimeMillis()
        val note = if (id == 0L) {
            NoteEntity(title = title, content = content, createdAt = now, updatedAt = now)
        } else {
            val existing = noteDao.getNoteById(id)
            NoteEntity(
                id = id,
                title = title,
                content = content,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        }
        return noteDao.insertNote(note)
    }

    suspend fun deleteNote(id: Long) = noteDao.deleteNoteById(id)

    suspend fun importNotes(notes: List<NoteEntity>) = noteDao.insertNotes(notes)

    // --- FİYAT ARA & KAYDET ---
    fun getRecentSearches(): Flow<List<String>> = cacheDao.getRecentSearches()

    suspend fun addRecentSearch(query: String) {
        if (query.isNotBlank()) {
            cacheDao.saveRecentSearch(RecentSearchEntity(query.trim()))
        }
    }

    suspend fun deleteRecentSearch(query: String) {
        cacheDao.deleteRecentSearch(query.trim())
    }

    fun getAllSavedPrices(): Flow<List<SavedPriceItemEntity>> = savedPriceDao.getAllSavedItems()

    fun isProductSaved(url: String): Flow<Boolean> = savedPriceDao.isItemSaved(url)

    suspend fun toggleSavePrice(item: PriceSearchResult, query: String = ""): Boolean {
        val isSaved = savedPriceDao.isItemSaved(item.productUrl).firstOrNull() ?: false
        if (isSaved) {
            savedPriceDao.deleteItem(item.productUrl)
            return false
        } else {
            savedPriceDao.insertItem(
                SavedPriceItemEntity(
                    productUrl = item.productUrl,
                    title = item.title,
                    storeName = item.storeName,
                    currentPrice = item.price,
                    originalPrice = item.price,
                    priceFormatted = item.priceFormatted,
                    imageUrl = item.imageUrl,
                    appPackageName = item.appPackageName,
                    query = query,
                    savedAt = System.currentTimeMillis()
                )
            )
            return true
        }
    }

    suspend fun removeSavedPrice(url: String) {
        savedPriceDao.deleteItem(url)
    }

    suspend fun clearAllSavedPrices() {
        savedPriceDao.deleteAll()
    }

    suspend fun refreshPrice(url: String): Pair<Double, String>? {
        return try {
            val (html, _) = DataFetcher.executeGetWithRetry(url)
            val doc = Jsoup.parse(html)
            val priceCandidate = doc.select(".price, span.price, span.newPrice, div.price, [data-testid='product-price']").text()
            val clean = cleanPrice(priceCandidate)
            if (clean > 0.0) {
                val formatted = DecimalFormat("#,##0.00 TL", DecimalFormatSymbols(Locale("tr", "TR"))).format(clean)
                savedPriceDao.updatePrice(url, clean, formatted, System.currentTimeMillis())
                Pair(clean, formatted)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun getStoreDiagnostics(): Map<String, StoreDiagnosticInfo> {
        val result = mutableMapOf<String, StoreDiagnosticInfo>()
        SourceConfig.PriceSearch.STORES.forEach { store ->
            val diag = PriceSearchFetcher.diagnosticMap[store.id]
                ?: StoreDiagnosticInfo(
                    storeId = store.id,
                    storeName = store.displayName,
                    layer = store.layer,
                    isEnabled = !PriceSearchFetcher.disabledStores.contains(store.id)
                )
            result[store.id] = diag.copy(isEnabled = !PriceSearchFetcher.disabledStores.contains(store.id))
        }
        return result
    }

    fun toggleStoreEnabled(storeId: String, isEnabled: Boolean) {
        if (isEnabled) {
            PriceSearchFetcher.disabledStores.remove(storeId)
        } else {
            PriceSearchFetcher.disabledStores.add(storeId)
        }
    }

    suspend fun searchPrices(
        query: String,
        onLayer1Results: (List<PriceSearchResult>) -> Unit,
        onFinalResults: (List<PriceSearchResult>) -> Unit
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return

        addRecentSearch(trimmedQuery)

        // 10 Dakikalık Önbellek Kontrolü
        val cached = cacheDao.getPriceSearchCache(trimmedQuery)
        val now = System.currentTimeMillis()
        if (cached != null && (now - cached.cachedAt) < 10 * 60 * 1000L) {
            val parsedList = parseCachedResults(cached.resultsJson)
            if (parsedList.isNotEmpty()) {
                onLayer1Results(parsedList)
                onFinalResults(parsedList)
                return
            }
        }

        val allResults = mutableListOf<PriceSearchResult>()

        coroutineScope {
            // KATMAN 1: Cimri ve Akakçe
            val layer1Stores = SourceConfig.PriceSearch.STORES.filter { it.layer == 1 }
            val layer1Deferreds = layer1Stores.map { store ->
                async { PriceSearchFetcher.searchStore(store, trimmedQuery) }
            }
            val layer1Outputs = layer1Deferreds.awaitAll().flatten()
            allResults.addAll(layer1Outputs)

            val rankedLayer1 = rankAndFilterResults(allResults)
            if (rankedLayer1.isNotEmpty()) {
                onLayer1Results(rankedLayer1)
            }

            // KATMAN 2: Mağaza siteleri
            val layer2Stores = SourceConfig.PriceSearch.STORES.filter { it.layer == 2 }
            val layer2Deferreds = layer2Stores.map { store ->
                async { PriceSearchFetcher.searchStore(store, trimmedQuery) }
            }
            val layer2Outputs = layer2Deferreds.awaitAll().flatten()
            allResults.addAll(layer2Outputs)

            val rankedWithLayer2 = rankAndFilterResults(allResults)
            onLayer1Results(rankedWithLayer2)

            // KATMAN 3: Büyük Pazar Yerleri
            val layer3Stores = SourceConfig.PriceSearch.STORES.filter { it.layer == 3 }
            val layer3Deferreds = layer3Stores.map { store ->
                async { PriceSearchFetcher.searchStore(store, trimmedQuery) }
            }
            val layer3Outputs = layer3Deferreds.awaitAll().flatten()
            allResults.addAll(layer3Outputs)

            val finalRanked = rankAndFilterResults(allResults)
            onFinalResults(finalRanked)

            saveSearchResultsToCache(trimmedQuery, finalRanked)
        }
    }

    private fun rankAndFilterResults(raw: List<PriceSearchResult>): List<PriceSearchResult> {
        val relevant = raw.filter { it.relevanceScore >= 60 }.sortedBy { it.price }

        val distinctStoresMap = mutableMapOf<String, PriceSearchResult>()
        for (item in relevant) {
            val key = item.storeName.lowercase()
            if (!distinctStoresMap.containsKey(key)) {
                distinctStoresMap[key] = item
            }
            if (distinctStoresMap.size >= 5) break
        }

        var top5 = distinctStoresMap.values.sortedBy { it.price }
        if (top5.size < 5) {
            val existingIds = top5.map { it.id }.toSet()
            val extra = relevant.filter { !existingIds.contains(it.id) }.take(5 - top5.size)
            top5 = (top5 + extra).sortedBy { it.price }
        }

        return top5.mapIndexed { index, item ->
            item.copy(isCheapest = index == 0)
        }
    }

    private suspend fun saveSearchResultsToCache(query: String, list: List<PriceSearchResult>) {
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("title", item.title)
                obj.put("storeId", item.storeId)
                obj.put("storeName", item.storeName)
                obj.put("price", item.price)
                obj.put("priceFormatted", item.priceFormatted)
                obj.put("productUrl", item.productUrl)
                obj.put("imageUrl", item.imageUrl ?: "")
                obj.put("appPackageName", item.appPackageName ?: "")
                obj.put("isCheapest", item.isCheapest)
                obj.put("layer", item.layer)
                obj.put("relevanceScore", item.relevanceScore)
                array.put(obj)
            }
            cacheDao.savePriceSearchCache(PriceSearchCacheEntity(query, array.toString()))
        } catch (_: Exception) {}
    }

    private fun parseCachedResults(json: String): List<PriceSearchResult> {
        val list = mutableListOf<PriceSearchResult>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PriceSearchResult(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        storeId = obj.optString("storeId", ""),
                        storeName = obj.optString("storeName", ""),
                        price = obj.optDouble("price", 0.0),
                        priceFormatted = obj.optString("priceFormatted", ""),
                        productUrl = obj.optString("productUrl", ""),
                        imageUrl = obj.optString("imageUrl", "").takeIf { it.isNotBlank() },
                        appPackageName = obj.optString("appPackageName", "").takeIf { it.isNotBlank() },
                        isCheapest = obj.optBoolean("isCheapest", false),
                        layer = obj.optInt("layer", 1),
                        relevanceScore = obj.optInt("relevanceScore", 100)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun cleanPrice(str: String): Double {
        if (str.isBlank()) return 0.0
        val sanitized = str.replace(".", "").replace(",", ".").replace("TL", "").replace("₺", "").trim()
        val match = Regex("\\d+\\.?\\d*").find(sanitized)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }
}
