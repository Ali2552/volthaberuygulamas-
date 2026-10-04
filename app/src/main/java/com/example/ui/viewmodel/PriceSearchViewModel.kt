package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PriceSearchResult
import com.example.data.model.PriceSearchState
import com.example.data.model.StoreDiagnosticInfo
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PriceSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)

    private val _uiState = MutableStateFlow(PriceSearchState())
    val uiState: StateFlow<PriceSearchState> = _uiState.asStateFlow()

    val recentSearches: StateFlow<List<String>> = repository.getRecentSearches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var searchJob: Job? = null

    init {
        refreshDiagnostics()
    }

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search(searchQuery: String = _uiState.value.query) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank()) return

        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            query = trimmed,
            isSearchingLayer1 = true,
            isSearchingBackground = false,
            errorMessage = null,
            searchedAtLeastOnce = true
        )

        searchJob = viewModelScope.launch {
            try {
                repository.searchPrices(
                    query = trimmed,
                    onLayer1Results = { l1Results ->
                        _uiState.value = _uiState.value.copy(
                            results = l1Results,
                            isSearchingLayer1 = false,
                            isSearchingBackground = true,
                            storeDiagnostics = repository.getStoreDiagnostics()
                        )
                    },
                    onFinalResults = { finalResults ->
                        _uiState.value = _uiState.value.copy(
                            results = finalResults,
                            isSearchingLayer1 = false,
                            isSearchingBackground = false,
                            storeDiagnostics = repository.getStoreDiagnostics(),
                            errorMessage = if (finalResults.isEmpty()) "Ürün bulunamadı. Aşağıdaki mağaza butonlarını kullanarak doğrudan mağazada arayabilirsiniz." else null
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearchingLayer1 = false,
                    isSearchingBackground = false,
                    errorMessage = "Arama sırasında bir hata oluştu: ${e.localizedMessage}"
                )
            }
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun refreshDiagnostics() {
        _uiState.value = _uiState.value.copy(
            storeDiagnostics = repository.getStoreDiagnostics()
        )
    }

    fun toggleStore(storeId: String, isEnabled: Boolean) {
        repository.toggleStoreEnabled(storeId, isEnabled)
        refreshDiagnostics()
    }

    fun getDiagnosticClipboardText(info: StoreDiagnosticInfo): String {
        return buildString {
            appendLine("=== T.K. BİLİŞİM MAĞAZA TANI BİLGİSİ ===")
            appendLine("Mağaza Adı: ${info.storeName} (${info.storeId})")
            appendLine("Katman: Katman ${info.layer}")
            appendLine("Durum: ${info.status.displayName}")
            appendLine("HTTP Durum Kodu: ${info.httpCode}")
            appendLine("İstek URL: ${info.requestUrl}")
            appendLine("Son Başarılı Zaman: ${info.lastSuccessTime ?: "Yok"}")
            appendLine("Bulunan Ürün Sayısı: ${info.foundCount}")
            appendLine("Eşleşmeyen / Bozuk Seçiciler: ${info.failedSelectors.joinToString(", ")}")
            appendLine("----------------------------------------")
            appendLine("HAM HTML KESİTİ (İlk 15000 karakter):")
            appendLine(info.rawHtmlSnippet.take(15000))
            appendLine("========================================")
        }
    }
}
