package com.example.data.remote

import android.util.Log
import com.example.data.config.SourceConfig
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.model.Article
import com.example.data.model.ElectricalCategory
import com.example.data.model.FetchMethod
import com.example.data.model.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

class ElectricFetcher(private val articleDao: ArticleDao) {

    suspend fun fetchAllSources(): FetchResult<List<Article>> = withContext(Dispatchers.IO) {
        val savedUrls = articleDao.getAllSavedUrls().toSet()

        coroutineScope {
            val eePowerDeferred = async {
                DataFetcher.fetchArticlesResilient(
                    sourceId = SourceConfig.Electrical.EEPower.ID,
                    sourceName = SourceConfig.Electrical.EEPower.DISPLAY_NAME,
                    baseUrl = SourceConfig.Electrical.EEPower.BASE_URL,
                    primaryRss = SourceConfig.Electrical.EEPower.PRIMARY_RSS,
                    fallbackRssList = SourceConfig.Electrical.EEPower.FALLBACK_RSS,
                    htmlSelector = SourceConfig.Electrical.EEPower.HTML_ARTICLE_SELECTOR,
                    isEnglish = true
                )
            }

            val aacDeferred = async {
                DataFetcher.fetchArticlesResilient(
                    sourceId = SourceConfig.Electrical.AllAboutCircuits.ID,
                    sourceName = SourceConfig.Electrical.AllAboutCircuits.DISPLAY_NAME,
                    baseUrl = SourceConfig.Electrical.AllAboutCircuits.BASE_URL,
                    primaryRss = SourceConfig.Electrical.AllAboutCircuits.PRIMARY_RSS,
                    fallbackRssList = SourceConfig.Electrical.AllAboutCircuits.FALLBACK_RSS,
                    htmlSelector = SourceConfig.Electrical.AllAboutCircuits.HTML_ARTICLE_SELECTOR,
                    isEnglish = true
                )
            }

            val elektrikHaberDeferred = async {
                DataFetcher.fetchArticlesResilient(
                    sourceId = SourceConfig.Electrical.ElektrikHaber.ID,
                    sourceName = SourceConfig.Electrical.ElektrikHaber.DISPLAY_NAME,
                    baseUrl = SourceConfig.Electrical.ElektrikHaber.BASE_URL,
                    primaryRss = SourceConfig.Electrical.ElektrikHaber.PRIMARY_RSS,
                    fallbackRssList = SourceConfig.Electrical.ElektrikHaber.FALLBACK_RSS,
                    htmlSelector = SourceConfig.Electrical.ElektrikHaber.HTML_ARTICLE_SELECTOR,
                    isEnglish = false
                )
            }

            val res1 = try { eePowerDeferred.await() } catch (e: Exception) { Log.e("ElectricFetcher", "EEPower error: ${e.message}"); null }
            val res2 = try { aacDeferred.await() } catch (e: Exception) { Log.e("ElectricFetcher", "AAC error: ${e.message}"); null }
            val res3 = try { elektrikHaberDeferred.await() } catch (e: Exception) { Log.e("ElectricFetcher", "ElektrikHaber error: ${e.message}"); null }

            val rawList = mutableListOf<Article>()
            var primaryMethod = FetchMethod.RSS

            res1?.let {
                rawList.addAll(it.data)
                if (it.method == FetchMethod.HTML) primaryMethod = FetchMethod.HTML
            }
            res2?.let {
                rawList.addAll(it.data)
                if (it.method == FetchMethod.HTML) primaryMethod = FetchMethod.HTML
            }
            res3?.let {
                rawList.addAll(it.data)
                if (it.method == FetchMethod.HTML) primaryMethod = FetchMethod.HTML
            }

            if (rawList.isNotEmpty()) {
                // Categorize articles by keyword matching and map saved state
                val categorized = rawList.distinctBy { it.url }.map { article ->
                    val cat = detectCategory(article.title, article.description)
                    article.copy(
                        category = cat,
                        isSaved = savedUrls.contains(article.url)
                    )
                }.sortedByDescending { it.timestamp }

                // Cache in database
                val entities = categorized.map {
                    ArticleEntity.fromArticle(it, isElectric = true, isSaved = it.isSaved)
                }
                articleDao.insertArticles(entities)

                FetchResult(categorized, primaryMethod)
            } else {
                // Load from cache
                val cached = articleDao.getCachedArticles(isElectric = true)
                    .map { it.toArticle(isSavedExplicit = savedUrls.contains(it.url), isOutOfDate = true) }

                if (cached.isNotEmpty()) {
                    FetchResult(
                        data = cached,
                        method = FetchMethod.CACHE,
                        isOutOfDate = true,
                        errorMessage = "Veri alınamadı, son önbellek gösteriliyor."
                    )
                } else {
                    FetchResult(
                        data = emptyList(),
                        method = FetchMethod.CACHE,
                        isOutOfDate = true,
                        errorMessage = "Elektrik sektörü haberleri yüklenemedi."
                    )
                }
            }
        }
    }

    private fun detectCategory(title: String, description: String): ElectricalCategory {
        val combined = "$title $description".lowercase(Locale.ROOT)

        // Güvenlik önceliği
        if (SourceConfig.Electrical.CATEGORY_KEYWORDS_SECURITY.any { combined.contains(it) }) {
            return ElectricalCategory.SECURITY
        }
        // Zayıf Akım
        if (SourceConfig.Electrical.CATEGORY_KEYWORDS_LOW_VOLTAGE.any { combined.contains(it) }) {
            return ElectricalCategory.LOW_VOLTAGE
        }
        // Kuvvetli Akım
        if (SourceConfig.Electrical.CATEGORY_KEYWORDS_HIGH_VOLTAGE.any { combined.contains(it) }) {
            return ElectricalCategory.HIGH_VOLTAGE
        }
        return ElectricalCategory.ALL
    }
}
