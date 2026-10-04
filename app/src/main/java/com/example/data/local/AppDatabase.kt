package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ArticleEntity::class,
        NoteEntity::class,
        WeatherCacheEntity::class,
        RecentSearchEntity::class,
        PriceSearchCacheEntity::class,
        SavedPriceItemEntity::class,
        FavoriteLocationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun noteDao(): NoteDao
    abstract fun cacheDao(): CacheDao
    abstract fun savedPriceDao(): SavedPriceDao
    abstract fun favoriteLocationDao(): FavoriteLocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Döviz tablolarını kaldır
                db.execSQL("DROP TABLE IF EXISTS currency_cache")
                db.execSQL("DROP TABLE IF EXISTS market_summary_cache")

                // MGM Hava Durumu Önbellek Tablosu
                db.execSQL("DROP TABLE IF EXISTS weather_cache")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS weather_cache (
                        id INTEGER PRIMARY KEY NOT NULL,
                        il TEXT NOT NULL,
                        ilce TEXT NOT NULL,
                        merkezId INTEGER NOT NULL,
                        istNo INTEGER NOT NULL,
                        currentTemp TEXT NOT NULL,
                        feelsLike TEXT NOT NULL,
                        condition TEXT NOT NULL,
                        humidity TEXT NOT NULL,
                        wind TEXT NOT NULL,
                        windDirection TEXT NOT NULL,
                        pressure TEXT NOT NULL,
                        iconType TEXT NOT NULL,
                        hourlyJson TEXT NOT NULL,
                        forecastsJson TEXT NOT NULL,
                        lastUpdated TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )""".trimIndent()
                )

                // Kaydedilen Fiyatlar Tablosu
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS saved_price_items (
                        productUrl TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        storeName TEXT NOT NULL,
                        currentPrice REAL NOT NULL,
                        originalPrice REAL NOT NULL,
                        priceFormatted TEXT NOT NULL,
                        imageUrl TEXT,
                        appPackageName TEXT,
                        query TEXT NOT NULL,
                        savedAt INTEGER NOT NULL,
                        lastCheckedAt INTEGER NOT NULL
                    )""".trimIndent()
                )

                // Favori Konumlar Tablosu
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS favorite_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        il TEXT NOT NULL,
                        ilce TEXT NOT NULL,
                        merkezId INTEGER NOT NULL,
                        istNo INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        savedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS currency_cache")
                db.execSQL("DROP TABLE IF EXISTS market_summary_cache")
                db.execSQL("DROP TABLE IF EXISTS weather_cache")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS weather_cache (
                        id INTEGER PRIMARY KEY NOT NULL,
                        il TEXT NOT NULL,
                        ilce TEXT NOT NULL,
                        merkezId INTEGER NOT NULL,
                        istNo INTEGER NOT NULL,
                        currentTemp TEXT NOT NULL,
                        feelsLike TEXT NOT NULL,
                        condition TEXT NOT NULL,
                        humidity TEXT NOT NULL,
                        wind TEXT NOT NULL,
                        windDirection TEXT NOT NULL,
                        pressure TEXT NOT NULL,
                        iconType TEXT NOT NULL,
                        hourlyJson TEXT NOT NULL,
                        forecastsJson TEXT NOT NULL,
                        lastUpdated TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS recent_searches (
                        `query` TEXT PRIMARY KEY NOT NULL,
                        timestamp INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS price_search_cache (
                        `query` TEXT PRIMARY KEY NOT NULL,
                        resultsJson TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS saved_price_items (
                        productUrl TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        storeName TEXT NOT NULL,
                        currentPrice REAL NOT NULL,
                        originalPrice REAL NOT NULL,
                        priceFormatted TEXT NOT NULL,
                        imageUrl TEXT,
                        appPackageName TEXT,
                        query TEXT NOT NULL,
                        savedAt INTEGER NOT NULL,
                        lastCheckedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS favorite_locations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        il TEXT NOT NULL,
                        ilce TEXT NOT NULL,
                        merkezId INTEGER NOT NULL,
                        istNo INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        savedAt INTEGER NOT NULL
                    )""".trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habervolt.db"
                )
                    .addMigrations(MIGRATION_1_3, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
