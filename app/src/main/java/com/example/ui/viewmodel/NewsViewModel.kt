package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.config.SourceConfig
import com.example.data.model.Article
import com.example.data.model.FetchMethod
import com.example.data.repository.AppRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NewsSourceFilter(val displayName: String) {
    ALL("Tümü"),
    BBC("BBC Türkçe"),
    SON_DAKIKA("Son Dakika")
}

enum class NewsTabMode(val displayName: String) {
    LATEST("Güncel"),
    SAVED("Kaydedilenler")
}

data class NewsUiState(
    val articles: List<Article> = emptyList(),
    val sourceFilter: NewsSourceFilter = NewsSourceFilter.ALL,
    val tabMode: NewsTabMode = NewsTabMode.LATEST,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOfflineWarning: Boolean = false,
    val bbcMethod: FetchMethod = FetchMethod.RSS,
    val sonDakikaMethod: FetchMethod = FetchMethod.RSS
)

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _uiState = MutableStateFlow(NewsUiState(isLoading = true))
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    val savedArticles: StateFlow<List<Article>> = repository.getSavedArticles(isElectric = false)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val bbcDeferred = async { repository.fetchBbcTurkce() }
            val sonDakikaDeferred = async { repository.fetchSonDakika() }

            val bbcResult = bbcDeferred.await()
            val sdResult = sonDakikaDeferred.await()

            val combined = mutableListOf<Article>()
            combined.addAll(bbcResult.data)
            combined.addAll(sdResult.data)

            // Deduplicate by URL and sort by timestamp
            val distinctList = combined.distinctBy { it.url }.sortedByDescending { it.timestamp }

            val anyOutOfDate = bbcResult.isOutOfDate || sdResult.isOutOfDate
            val anyFailure = bbcResult.errorMessage != null && sdResult.errorMessage != null

            _uiState.value = _uiState.value.copy(
                articles = distinctList,
                isLoading = false,
                isOfflineWarning = anyOutOfDate,
                errorMessage = if (distinctList.isEmpty() && anyFailure) "Haberler yüklenemedi. Lütfen internetinizi kontrol edin." else null,
                bbcMethod = bbcResult.method,
                sonDakikaMethod = sdResult.method
            )
        }
    }

    fun setSourceFilter(filter: NewsSourceFilter) {
        _uiState.value = _uiState.value.copy(sourceFilter = filter)
    }

    fun setTabMode(mode: NewsTabMode) {
        _uiState.value = _uiState.value.copy(tabMode = mode)
    }

    fun toggleSave(article: Article) {
        viewModelScope.launch {
            val isNowSaved = repository.toggleSaveArticle(article, isElectric = false)
            _uiState.value = _uiState.value.copy(
                articles = _uiState.value.articles.map {
                    if (it.url == article.url) it.copy(isSaved = isNowSaved) else it
                }
            )
        }
    }

    fun deleteSavedArticle(url: String) {
        viewModelScope.launch {
            repository.deleteSavedArticle(url)
            _uiState.value = _uiState.value.copy(
                articles = _uiState.value.articles.map {
                    if (it.url == url) it.copy(isSaved = false) else it
                }
            )
        }
    }
}
