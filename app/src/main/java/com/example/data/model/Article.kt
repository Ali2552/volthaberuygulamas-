package com.example.data.model

enum class ElectricalCategory(val title: String) {
    ALL("Tümü"),
    LOW_VOLTAGE("Zayıf Akım"),
    HIGH_VOLTAGE("Kuvvetli Akım"),
    SECURITY("Güvenlik")
}

data class Article(
    val url: String,
    val title: String,
    val description: String = "",
    val sourceId: String,
    val sourceName: String,
    val publishedDate: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val isEnglish: Boolean = false,
    val category: ElectricalCategory = ElectricalCategory.ALL,
    val isSaved: Boolean = false,
    val fetchMethod: FetchMethod = FetchMethod.RSS,
    val isOutOfDate: Boolean = false
)
