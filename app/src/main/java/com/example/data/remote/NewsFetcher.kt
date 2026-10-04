package com.example.data.remote

import com.example.data.config.SourceConfig
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.model.Article
import com.example.data.model.FetchMethod
import com.example.data.model.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewsFetcher(private val articleDao: ArticleDao) {

    suspend fun fetchBbcTurkce(): FetchResult<List<Article>> = withContext(Dispatchers.IO) {
        val result = DataFetcher.fetchArticlesResilient(
            sourceId = SourceConfig.News.BbcTurkce.ID,
            sourceName = SourceConfig.News.BbcTurkce.DISPLAY_NAME,
            baseUrl = SourceConfig.News.BbcTurkce.BASE_URL,
            primaryRss = SourceConfig.News.BbcTurkce.PRIMARY_RSS,
            fallbackRssList = SourceConfig.News.BbcTurkce.FALLBACK_RSS,
            htmlSelector = SourceConfig.News.BbcTurkce.HTML_ARTICLE_SELECTOR,
            titleSelector = SourceConfig.News.BbcTurkce.HTML_TITLE_SELECTOR
        )

        handleResultAndCache(result, SourceConfig.News.BbcTurkce.ID)
    }

    suspend fun fetchSonDakika(): FetchResult<List<Article>> = withContext(Dispatchers.IO) {
        val result = DataFetcher.fetchArticlesResilient(
            sourceId = SourceConfig.News.SonDakika.ID,
            sourceName = SourceConfig.News.SonDakika.DISPLAY_NAME,
            baseUrl = SourceConfig.News.SonDakika.BASE_URL,
            primaryRss = SourceConfig.News.SonDakika.PRIMARY_RSS,
            fallbackRssList = SourceConfig.News.SonDakika.FALLBACK_RSS,
            htmlSelector = SourceConfig.News.SonDakika.HTML_ARTICLE_SELECTOR,
            titleSelector = SourceConfig.News.SonDakika.HTML_TITLE_SELECTOR
        )

        handleResultAndCache(result, SourceConfig.News.SonDakika.ID)
    }

    private suspend fun handleResultAndCache(
        result: FetchResult<List<Article>>,
        sourceId: String
    ): FetchResult<List<Article>> {
        val savedUrls = articleDao.getAllSavedUrls().toSet()

        if (result.data.isNotEmpty()) {
            // Mark saved state
            val mapped = result.data.map {
                it.copy(isSaved = savedUrls.contains(it.url))
            }
            // Cache to database
            val entities = mapped.map {
                ArticleEntity.fromArticle(it, isElectric = false, isSaved = it.isSaved)
            }
            articleDao.insertArticles(entities)
            return result.copy(data = mapped)
        } else {
            // Failed: fallback to cached articles
            val cached = articleDao.getCachedArticles(isElectric = false)
                .filter { it.sourceId == sourceId }
                .map { it.toArticle(isSavedExplicit = savedUrls.contains(it.url), isOutOfDate = true) }

            return if (cached.isNotEmpty()) {
                FetchResult(
                    data = cached,
                    method = FetchMethod.CACHE,
                    isOutOfDate = true,
                    errorMessage = "Güncel veri alınamadı, önbellek gösteriliyor."
                )
            } else {
                result
            }
        }
    }
}
