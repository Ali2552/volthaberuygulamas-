package com.example.data.model

enum class StoreStatus(val displayName: String) {
    SUCCESS("Başarılı"),
    BLOCKED("Engellendi (403/429/Bot)"),
    SELECTOR_BROKEN("Seçici Bozuk"),
    TIMEOUT("Zaman Aşımı"),
    DISABLED("Devre Dışı")
}

data class StoreDiagnosticInfo(
    val storeId: String,
    val storeName: String,
    val layer: Int,
    val status: StoreStatus = StoreStatus.SUCCESS,
    val lastSuccessTime: String? = null,
    val foundCount: Int = 0,
    val httpCode: Int = 200,
    val requestUrl: String = "",
    val failedSelectors: List<String> = emptyList(),
    val rawHtmlSnippet: String = "",
    val isEnabled: Boolean = true
)

data class PriceSearchResult(
    val id: String,
    val title: String,
    val storeId: String,
    val storeName: String,
    val price: Double,
    val priceFormatted: String,
    val productUrl: String,
    val imageUrl: String? = null,
    val appPackageName: String? = null,
    val isCheapest: Boolean = false,
    val layer: Int = 1,
    val relevanceScore: Int = 100
)

data class PriceSearchState(
    val query: String = "",
    val results: List<PriceSearchResult> = emptyList(),
    val isSearchingLayer1: Boolean = false,
    val isSearchingBackground: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val storeDiagnostics: Map<String, StoreDiagnosticInfo> = emptyMap(),
    val disabledStores: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val searchedAtLeastOnce: Boolean = false
)
