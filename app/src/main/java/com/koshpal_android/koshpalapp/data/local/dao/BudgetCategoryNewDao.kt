package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.koshpal_android.koshpalapp.model.BudgetCategory

@Dao
interface BudgetCategoryNewDao {
    @Query("SELECT * FROM budget_categories WHERE budgetId = :budgetId ORDER BY id ASC")
    suspend fun getCategoriesForBudget(budgetId: Int): List<BudgetCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<BudgetCategory>)

    @Update
    suspend fun update(category: BudgetCategory)

    @Query("DELETE FROM budget_categories WHERE budgetId = :budgetId")
    suspend fun clearForBudget(budgetId: Int)
}


