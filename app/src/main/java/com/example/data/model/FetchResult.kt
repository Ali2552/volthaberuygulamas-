package com.example.data.model

enum class FetchMethod(val label: String) {
    RSS("RSS"),
    HTML("HTML"),
    API("API"),
    CACHE("Önbellek")
}

data class FetchResult<T>(
    val data: T,
    val method: FetchMethod,
    val isOutOfDate: Boolean = false,
    val errorMessage: String? = null
)
