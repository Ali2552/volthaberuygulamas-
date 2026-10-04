package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {
    @Query("SELECT * FROM weather_cache WHERE id = 1 LIMIT 1")
    suspend fun getWeatherCache(): WeatherCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWeatherCache(weather: WeatherCacheEntity)

    // Recent Searches
    @Query("SELECT `query` FROM recent_searches ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecentSearch(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    // Price Search Cache (10 minutes validity)
    @Query("SELECT * FROM price_search_cache WHERE `query` = :query LIMIT 1")
    suspend fun getPriceSearchCache(query: String): PriceSearchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePriceSearchCache(cache: PriceSearchCacheEntity)
}
