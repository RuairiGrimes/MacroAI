package com.macroai.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.macroai.tracker.data.local.entity.SavedMealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMealDao {

    @Query("SELECT * FROM saved_meals ORDER BY lastUsedAt DESC, createdAt DESC")
    fun observeAllSavedMeals(): Flow<List<SavedMealEntity>>

    @Query("SELECT * FROM saved_meals ORDER BY lastUsedAt DESC, createdAt DESC LIMIT 100")
    suspend fun getAllSavedMealsList(): List<SavedMealEntity>

    @Query("SELECT * FROM saved_meals WHERE id = :id")
    suspend fun getSavedMealById(id: String): SavedMealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedMeal(meal: SavedMealEntity)

    @Update
    suspend fun updateSavedMeal(meal: SavedMealEntity)

    @Query("DELETE FROM saved_meals WHERE id = :id")
    suspend fun deleteSavedMealById(id: String)
}

