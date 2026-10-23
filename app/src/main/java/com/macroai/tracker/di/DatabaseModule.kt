package com.macroai.tracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.macroai.tracker.data.local.MacroAiDatabase
import com.macroai.tracker.data.local.dao.FoodLogDao
import com.macroai.tracker.data.local.dao.SavedMealDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE food_logs ADD COLUMN ingredientBreakdownJson TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE food_logs ADD COLUMN recipeId TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE food_logs ADD COLUMN servingsConsumed REAL NOT NULL DEFAULT 1.0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS saved_meals (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    totalServings REAL NOT NULL,
                    totalWeightGrams REAL NOT NULL,
                    ingredientsJson TEXT NOT NULL,
                    totalCalories REAL NOT NULL,
                    totalProteinG REAL NOT NULL,
                    totalCarbsG REAL NOT NULL,
                    totalFatG REAL NOT NULL,
                    totalSaturatedFatG REAL NOT NULL,
                    totalPolyunsaturatedFatG REAL NOT NULL,
                    totalMonounsaturatedFatG REAL NOT NULL,
                    totalSugarG REAL NOT NULL,
                    totalFibreG REAL NOT NULL,
                    novaGroup INTEGER,
                    isUpf INTEGER NOT NULL,
                    containsSeedOils INTEGER NOT NULL,
                    detectedSeedOils TEXT NOT NULL,
                    sodiumMg REAL NOT NULL,
                    potassiumMg REAL NOT NULL,
                    ironMg REAL NOT NULL,
                    calciumMg REAL NOT NULL,
                    vitaminCMg REAL NOT NULL,
                    vitaminDUg REAL NOT NULL,
                    magnesiumMg REAL NOT NULL,
                    createdAt INTEGER NOT NULL,
                    lastUsedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS batch_preps (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    recipeId TEXT,
                    prepDate INTEGER NOT NULL,
                    rawIngredientsWeightGrams REAL NOT NULL,
                    cookedTotalWeightGrams REAL NOT NULL,
                    totalCalories REAL NOT NULL,
                    totalProteinG REAL NOT NULL,
                    totalCarbsG REAL NOT NULL,
                    totalFatG REAL NOT NULL,
                    totalSaturatedFatG REAL NOT NULL,
                    totalPolyunsaturatedFatG REAL NOT NULL,
                    totalMonounsaturatedFatG REAL NOT NULL,
                    totalSugarG REAL NOT NULL,
                    totalFibreG REAL NOT NULL,
                    novaGroup INTEGER,
                    isUpf INTEGER NOT NULL,
                    containsSeedOils INTEGER NOT NULL,
                    detectedSeedOils TEXT NOT NULL,
                    totalContainers INTEGER NOT NULL,
                    containersRemaining INTEGER NOT NULL,
                    targetGramsPerContainer REAL NOT NULL,
                    ingredientsJson TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    isArchived INTEGER NOT NULL,
                    lastLoggedAt INTEGER
                )
                """.trimIndent()
            )
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS batch_preps")
        }
    }

    @Provides
    @Singleton
    fun provideMacroAiDatabase(
        @ApplicationContext context: Context
    ): MacroAiDatabase {
        return Room.databaseBuilder(
            context,
            MacroAiDatabase::class.java,
            MacroAiDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideFoodLogDao(database: MacroAiDatabase): FoodLogDao {
        return database.foodLogDao()
    }

    @Provides
    @Singleton
    fun provideSavedMealDao(database: MacroAiDatabase): SavedMealDao {
        return database.savedMealDao()
    }
}
