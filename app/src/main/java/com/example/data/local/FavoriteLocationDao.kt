package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteLocationDao {
    @Query("SELECT * FROM favorite_locations ORDER BY savedAt DESC LIMIT 10")
    fun getAllFavorites(): Flow<List<FavoriteLocationEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_locations WHERE merkezId = :merkezId)")
    fun isFavorite(merkezId: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM favorite_locations")
    suspend fun getFavoriteCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(location: FavoriteLocationEntity)

    @Query("DELETE FROM favorite_locations WHERE merkezId = :merkezId")
    suspend fun deleteFavoriteByMerkezId(merkezId: Int)

    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)
}
