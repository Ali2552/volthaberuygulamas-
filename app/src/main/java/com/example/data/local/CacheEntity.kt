package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey val id: Int = 1,
    val il: String = "İzmir",
    val ilce: String = "Merkez",
    val merkezId: Int = 93500,
    val istNo: Int = 17220,
    val currentTemp: String,
    val feelsLike: String,
    val condition: String,
    val humidity: String,
    val wind: String,
    val windDirection: String,
    val pressure: String,
    val iconType: String,
    val hourlyJson: String,
    val forecastsJson: String,
    val lastUpdated: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "price_search_cache")
data class PriceSearchCacheEntity(
    @PrimaryKey val query: String,
    val resultsJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_price_items")
data class SavedPriceItemEntity(
    @PrimaryKey val productUrl: String,
    val title: String,
    val storeName: String,
    val currentPrice: Double,
    val originalPrice: Double, // Price when user first saved it
    val priceFormatted: String,
    val imageUrl: String? = null,
    val appPackageName: String? = null,
    val query: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val lastCheckedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_locations")
data class FavoriteLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val il: String,
    val ilce: String,
    val merkezId: Int,
    val istNo: Int,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val savedAt: Long = System.currentTimeMillis()
)
