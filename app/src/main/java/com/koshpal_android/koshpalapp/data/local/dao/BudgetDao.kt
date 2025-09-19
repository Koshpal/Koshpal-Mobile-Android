package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY monthlyLimit DESC")
    fun getAllActiveBudgets(): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year AND isActive = 1")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>
    
    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: String): Budget?
    
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year AND isActive = 1")
    suspend fun getBudgetByCategoryAndMonth(categoryId: String, month: Int, year: Int): Budget?
    
    @Query("""
        SELECT * FROM budgets 
        WHERE categoryId = :categoryId AND isActive = 1 
        ORDER BY year DESC, month DESC 
        LIMIT 1
    """)
    suspend fun getLatestBudgetForCategory(categoryId: String): Budget?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)
    
    @Update
    suspend fun updateBudget(budget: Budget)
    
    @Delete
    suspend fun deleteBudget(budget: Budget)
    
    @Query("UPDATE budgets SET spent = :spent, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBudgetSpent(id: String, spent: Double, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBudget(id: String)
    
    @Query("SELECT * FROM budgets WHERE spent / monthlyLimit >= 0.8 AND isActive = 1")
    suspend fun getBudgetsNearLimit(): List<Budget>
    
    @Query("SELECT * FROM budgets WHERE spent > monthlyLimit AND isActive = 1")
    suspend fun getExceededBudgets(): List<Budget>
    
    @Query("SELECT SUM(monthlyLimit) FROM budgets WHERE month = :month AND year = :year AND isActive = 1")
    suspend fun getTotalBudgetForMonth(month: Int, year: Int): Double?
    
    @Query("SELECT SUM(spent) FROM budgets WHERE month = :month AND year = :year AND isActive = 1")
    suspend fun getTotalSpentForMonth(month: Int, year: Int): Double?
}
