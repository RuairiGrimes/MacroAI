package com.macroai.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.macroai.tracker.data.local.converters.Converters
import com.macroai.tracker.data.local.dao.FoodLogDao
import com.macroai.tracker.data.local.dao.SavedMealDao
import com.macroai.tracker.data.local.entity.FoodLogEntity
import com.macroai.tracker.data.local.entity.SavedMealEntity

@Database(
    entities = [
        FoodLogEntity::class,
        SavedMealEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MacroAiDatabase : RoomDatabase() {
    abstract fun foodLogDao(): FoodLogDao
    abstract fun savedMealDao(): SavedMealDao

    companion object {
        const val DATABASE_NAME = "macro_ai_db"
    }
}
