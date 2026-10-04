package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Article
import com.example.data.model.ElectricalCategory
import com.example.data.model.FetchMethod

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String = "",
    val sourceId: String,
    val sourceName: String,
    val publishedDate: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val isEnglish: Boolean = false,
    val category: String = ElectricalCategory.ALL.name,
    val isSaved: Boolean = false,
    val isElectricNews: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toArticle(isSavedExplicit: Boolean = isSaved, isOutOfDate: Boolean = false): Article {
        return Article(
            url = url,
            title = title,
            description = description,
            sourceId = sourceId,
            sourceName = sourceName,
            publishedDate = publishedDate,
            timestamp = timestamp,
            imageUrl = imageUrl,
            isEnglish = isEnglish,
            category = try {
                ElectricalCategory.valueOf(category)
            } catch (e: Exception) {
                ElectricalCategory.ALL
            },
            isSaved = isSavedExplicit,
            fetchMethod = FetchMethod.CACHE,
            isOutOfDate = isOutOfDate
        )
    }

    companion object {
        fun fromArticle(article: Article, isElectric: Boolean, isSaved: Boolean = false): ArticleEntity {
            return ArticleEntity(
                url = article.url,
                title = article.title,
                description = article.description,
                sourceId = article.sourceId,
                sourceName = article.sourceName,
                publishedDate = article.publishedDate,
                timestamp = article.timestamp,
                imageUrl = article.imageUrl,
                isEnglish = article.isEnglish,
                category = article.category.name,
                isSaved = isSaved,
                isElectricNews = isElectric,
                cachedAt = System.currentTimeMillis()
            )
        }
    }
}
