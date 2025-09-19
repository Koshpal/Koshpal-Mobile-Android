package com.koshpal_android.koshpalapp.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.model.BudgetStatus
import com.koshpal_android.koshpalapp.model.GoalCategory

class Converters {
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }
    
    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }
    
    @TypeConverter
    fun fromBudgetStatus(value: BudgetStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toBudgetStatus(value: String): BudgetStatus {
        return BudgetStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromGoalCategory(value: GoalCategory): String {
        return value.name
    }
    
    @TypeConverter
    fun toGoalCategory(value: String): GoalCategory {
        return GoalCategory.valueOf(value)
    }
}
