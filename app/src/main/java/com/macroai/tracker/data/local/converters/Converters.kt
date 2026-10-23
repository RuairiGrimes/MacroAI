package com.macroai.tracker.data.local.converters

import androidx.room.TypeConverter
import com.macroai.tracker.domain.model.LogSource
import com.macroai.tracker.domain.model.MealType

class Converters {
    @TypeConverter
    fun fromMealType(mealType: MealType): String = mealType.name

    @TypeConverter
    fun toMealType(value: String): MealType =
        runCatching { MealType.valueOf(value) }.getOrDefault(MealType.SNACK)

    @TypeConverter
    fun fromLogSource(source: LogSource): String = source.name

    @TypeConverter
    fun toLogSource(value: String): LogSource =
        runCatching { LogSource.valueOf(value) }.getOrDefault(LogSource.MANUAL)
}

