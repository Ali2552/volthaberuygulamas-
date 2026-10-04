package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Article
import com.example.data.model.ElectricalCategory
import com.example.data.model.FetchMethod
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ElectricUiState(
    val articles: List<Article> = emptyList(),
    val selectedCategory: ElectricalCategory = ElectricalCategory.ALL,
    val isTodayOnly: Boolean = false,
    val tabMode: NewsTabMode = NewsTabMode.LATEST,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOfflineWarning: Boolean = false,
    val fetchMethod: FetchMethod = FetchMethod.RSS
)

class ElectricViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _uiState = MutableStateFlow(ElectricUiState(isLoading = true))
    val uiState: StateFlow<ElectricUiState> = _uiState.asStateFlow()

    val savedArticles: StateFlow<List<Article>> = repository.getSavedArticles(isElectric = true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.fetchElectricalArticles()
            _uiState.value = _uiState.value.copy(
                articles = result.data,
                isLoading = false,
                isOfflineWarning = result.isOutOfDate,
                errorMessage = if (result.data.isEmpty()) result.errorMessage else null,
                fetchMethod = result.method
            )
        }
    }

    fun setCategory(category: ElectricalCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun toggleTodayOnly() {
        _uiState.value = _uiState.value.copy(isTodayOnly = !_uiState.value.isTodayOnly)
    }

    fun setTabMode(mode: NewsTabMode) {
        _uiState.value = _uiState.value.copy(tabMode = mode)
    }

    fun toggleSave(article: Article) {
        viewModelScope.launch {
            val isNowSaved = repository.toggleSaveArticle(article, isElectric = true)
            _uiState.value = _uiState.value.copy(
                articles = _uiState.value.articles.map {
                    if (it.url == article.url) it.copy(isSaved = isNowSaved) else it
                }
            )
        }
    }

    fun deleteSaved(url: String) {
        viewModelScope.launch {
            repository.deleteSavedArticle(url)
            _uiState.value = _uiState.value.copy(
                articles = _uiState.value.articles.map {
                    if (it.url == url) it.copy(isSaved = false) else it
                }
            )
        }
    }

    fun filterArticles(list: List<Article>): List<Article> {
        val cat = _uiState.value.selectedCategory
        val todayOnly = _uiState.value.isTodayOnly

        var filtered = if (cat == ElectricalCategory.ALL) {
            list
        } else {
            list.filter { it.category == cat }
        }

        if (todayOnly) {
            val midnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            filtered = filtered.filter {
                it.timestamp >= midnight || it.publishedDate.lowercase().contains("bugün") || it.publishedDate.lowercase().contains("today")
            }
        }
        return filtered
    }
}
