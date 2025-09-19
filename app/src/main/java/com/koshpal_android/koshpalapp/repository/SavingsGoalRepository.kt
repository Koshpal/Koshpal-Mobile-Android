package com.koshpal_android.koshpalapp.repository

import com.koshpal_android.koshpalapp.data.local.dao.SavingsGoalDao
import com.koshpal_android.koshpalapp.model.SavingsGoal
import com.koshpal_android.koshpalapp.model.GoalCategory
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepository @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao
) {
    
    fun getAllActiveGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllActiveGoals()
    }
    
    fun getAllGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllGoals()
    }
    
    suspend fun getGoalById(id: String): SavingsGoal? {
        return savingsGoalDao.getGoalById(id)
    }
    
    fun getGoalsByCategory(category: GoalCategory): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getGoalsByCategory(category)
    }
    
    fun getCompletedGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getCompletedGoals()
    }
    
    fun getActiveGoals(): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getActiveGoals()
    }
    
    suspend fun createGoal(
        name: String, 
        targetAmount: Double, 
        category: GoalCategory,
        targetDate: Long? = null
    ): SavingsGoal {
        val goal = SavingsGoal(
            id = UUID.randomUUID().toString(),
            name = name,
            targetAmount = targetAmount,
            category = category,
            targetDate = targetDate
        )
        savingsGoalDao.insertGoal(goal)
        return goal
    }
    
    suspend fun updateGoal(goal: SavingsGoal) {
        savingsGoalDao.updateGoal(goal)
    }
    
    suspend fun deleteSavingsGoal(goal: SavingsGoal) {
        savingsGoalDao.deleteSavingsGoal(goal)
    }
    
    suspend fun getActiveSavingsGoalsCount(): Int {
        return savingsGoalDao.getActiveSavingsGoalsCount()
    }
    
    suspend fun addToGoal(goalId: String, amount: Double) {
        savingsGoalDao.addToGoal(goalId, amount)
    }
    
    suspend fun updateGoalAmount(goalId: String, amount: Double) {
        savingsGoalDao.updateGoalAmount(goalId, amount)
    }
    
    suspend fun deactivateGoal(goalId: String) {
        savingsGoalDao.deactivateGoal(goalId)
    }
    
    suspend fun calculateTimeToGoal(goal: SavingsGoal, monthlyContribution: Double): Int {
        if (monthlyContribution <= 0) return -1
        
        val remainingAmount = goal.remainingAmount
        return (remainingAmount / monthlyContribution).toInt() + 1
    }
    
    suspend fun getOverdueGoals(): List<SavingsGoal> {
        return savingsGoalDao.getOverdueGoals()
    }
    
    suspend fun getTotalTargetAmount(): Double {
        return savingsGoalDao.getTotalTargetAmount() ?: 0.0
    }
    
    suspend fun getTotalSavedAmount(): Double {
        return savingsGoalDao.getTotalSavedAmount() ?: 0.0
    }
    
    suspend fun getCompletedGoalsCount(): Int {
        return savingsGoalDao.getCompletedGoalsCount()
    }
    
    suspend fun getGoalProgress(): Triple<Double, Double, Float> {
        val totalTarget = getTotalTargetAmount()
        val totalSaved = getTotalSavedAmount()
        val progressPercentage = if (totalTarget > 0) (totalSaved / totalTarget * 100).toFloat() else 0f
        
        return Triple(totalTarget, totalSaved, progressPercentage)
    }
}
