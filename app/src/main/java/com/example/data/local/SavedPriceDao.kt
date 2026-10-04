package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPriceDao {
    @Query("SELECT * FROM saved_price_items ORDER BY savedAt DESC")
    fun getAllSavedItems(): Flow<List<SavedPriceItemEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_price_items WHERE productUrl = :url)")
    fun isItemSaved(url: String): Flow<Boolean>

    @Query("SELECT productUrl FROM saved_price_items")
    fun getAllSavedUrls(): Flow<List<String>>

    @Query("SELECT * FROM saved_price_items WHERE title LIKE '%' || :query || '%' OR storeName LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    fun searchSavedItems(query: String): Flow<List<SavedPriceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SavedPriceItemEntity)

    @Query("DELETE FROM saved_price_items WHERE productUrl = :url")
    suspend fun deleteItem(url: String)

    @Query("DELETE FROM saved_price_items")
    suspend fun deleteAll()

    @Query("UPDATE saved_price_items SET currentPrice = :newPrice, priceFormatted = :formatted, lastCheckedAt = :checkedAt WHERE productUrl = :url")
    suspend fun updatePrice(url: String, newPrice: Double, formatted: String, checkedAt: Long)
}
