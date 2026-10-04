package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles WHERE isSaved = 1 AND isElectricNews = :isElectric ORDER BY timestamp DESC")
    fun getSavedArticles(isElectric: Boolean): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isElectricNews = :isElectric ORDER BY timestamp DESC LIMIT 50")
    suspend fun getCachedArticles(isElectric: Boolean): List<ArticleEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE url = :url AND isSaved = 1)")
    fun isArticleSavedFlow(url: String): Flow<Boolean>

    @Query("SELECT url FROM articles WHERE isSaved = 1")
    fun getAllSavedUrlsFlow(): Flow<List<String>>

    @Query("SELECT url FROM articles WHERE isSaved = 1")
    suspend fun getAllSavedUrls(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("UPDATE articles SET isSaved = :isSaved WHERE url = :url")
    suspend fun updateSavedStatus(url: String, isSaved: Boolean)

    @Query("DELETE FROM articles WHERE url = :url")
    suspend fun deleteArticle(url: String)
}
