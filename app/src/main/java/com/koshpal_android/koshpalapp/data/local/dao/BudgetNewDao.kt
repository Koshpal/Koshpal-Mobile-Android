package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.koshpal_android.koshpalapp.model.Budget

@Dao
interface BudgetNewDao {
    @Query("SELECT * FROM budgets LIMIT 1")
    suspend fun getSingleBudget(): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()
}


