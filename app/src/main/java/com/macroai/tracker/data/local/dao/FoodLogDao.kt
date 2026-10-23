package com.macroai.tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.macroai.tracker.data.local.entity.FoodLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodLogDao {

    @Query(
        """
        SELECT * FROM food_logs 
        WHERE timestamp >= :startOfDayMillis AND timestamp < :endOfDayMillis 
        ORDER BY timestamp DESC
        """
    )
    fun observeLogsForDateRange(
        startOfDayMillis: Long,
        endOfDayMillis: Long
    ): Flow<List<FoodLogEntity>>

    @Query(
        """
        SELECT * FROM food_logs 
        WHERE timestamp >= :startOfDayMillis AND timestamp < :endOfDayMillis 
        ORDER BY timestamp DESC
        """
    )
    suspend fun getLogsForDateRange(
        startOfDayMillis: Long,
        endOfDayMillis: Long
    ): List<FoodLogEntity>

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun observeAllLogs(): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE id = :id LIMIT 1")
    suspend fun getFoodLogById(id: String): FoodLogEntity?

    @Query("SELECT * FROM food_logs WHERE id = :id LIMIT 1")
    fun observeFoodLogById(id: String): Flow<FoodLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(entity: FoodLogEntity)

    @Update
    suspend fun updateFoodLog(entity: FoodLogEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFoodLogById(id: String)

    @Query("SELECT * FROM food_logs WHERE isSyncedToSheets = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLogs(): List<FoodLogEntity>

    @Query("SELECT COUNT(*) FROM food_logs WHERE isSyncedToSheets = 0")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("UPDATE food_logs SET isSyncedToSheets = 1 WHERE id IN (:ids)")
    suspend fun markLogsAsSynced(ids: List<String>)

    @Query("SELECT * FROM food_logs GROUP BY name ORDER BY timestamp DESC LIMIT 150")
    suspend fun getRecentDistinctFoodLogs(): List<FoodLogEntity>
}

