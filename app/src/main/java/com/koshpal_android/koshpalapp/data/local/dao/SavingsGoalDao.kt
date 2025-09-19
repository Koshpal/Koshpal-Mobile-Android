package com.koshpal_android.koshpalapp.data.local.dao

import androidx.room.*
import com.koshpal_android.koshpalapp.model.SavingsGoal
import com.koshpal_android.koshpalapp.model.GoalCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    
    @Query("SELECT * FROM savings_goals WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActiveGoals(): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: String): SavingsGoal?
    
    @Query("SELECT * FROM savings_goals WHERE category = :category AND isActive = 1")
    fun getGoalsByCategory(category: GoalCategory): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals WHERE currentAmount >= targetAmount AND isActive = 1")
    fun getCompletedGoals(): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals WHERE currentAmount < targetAmount AND isActive = 1")
    fun getActiveGoals(): Flow<List<SavingsGoal>>
    
    @Query("SELECT * FROM savings_goals WHERE targetDate IS NOT NULL AND targetDate < :currentTime AND currentAmount < targetAmount AND isActive = 1")
    suspend fun getOverdueGoals(currentTime: Long = System.currentTimeMillis()): List<SavingsGoal>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<SavingsGoal>)
    
    @Update
    suspend fun updateGoal(goal: SavingsGoal)
    
    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)
    
    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :amount, updatedAt = :updatedAt WHERE id = :goalId")
    suspend fun addToGoal(goalId: String, amount: Double, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE savings_goals SET currentAmount = :amount, updatedAt = :updatedAt WHERE id = :goalId")
    suspend fun updateGoalAmount(goalId: String, amount: Double, updatedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM savings_goals WHERE isActive = 1")
    suspend fun getActiveSavingsGoalsCount(): Int
    
    @Query("UPDATE savings_goals SET isActive = 0 WHERE id = :id")
    suspend fun deactivateGoal(id: String)
    
    @Query("SELECT SUM(targetAmount) FROM savings_goals WHERE isActive = 1")
    suspend fun getTotalTargetAmount(): Double?
    
    @Query("SELECT SUM(currentAmount) FROM savings_goals WHERE isActive = 1")
    suspend fun getTotalSavedAmount(): Double?
    
    @Query("SELECT COUNT(*) FROM savings_goals WHERE currentAmount >= targetAmount AND isActive = 1")
    suspend fun getCompletedGoalsCount(): Int
}
