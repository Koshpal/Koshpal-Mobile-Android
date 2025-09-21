package com.koshpal_android.koshpalapp.repository

import com.koshpal_android.koshpalapp.data.local.dao.TransactionDao
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao
) {
    
    fun getAllActiveBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllActiveBudgets()
    }
    
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsForMonth(month, year)
    }
    
    suspend fun getBudgetById(id: String): Budget? {
        return budgetDao.getBudgetById(id)
    }
    
    suspend fun getBudgetByCategoryAndMonth(categoryId: String, month: Int, year: Int): Budget? {
        return budgetDao.getBudgetByCategoryAndMonth(categoryId, month, year)
    }
    
    suspend fun createBudget(categoryId: String, limit: Double, month: Int, year: Int): Budget {
        val budget = Budget(
            id = UUID.randomUUID().toString(),
            categoryId = categoryId,
            monthlyLimit = limit,
            month = month,
            year = year
        )
        budgetDao.insertBudget(budget)
        return budget
    }
    
    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }
    
    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
    
    suspend fun updateBudgetSpending(budgetId: String, additionalSpent: Double) {
        val budget = getBudgetById(budgetId)
        if (budget != null) {
            val newSpent = budget.spent + additionalSpent
            budgetDao.updateBudgetSpent(budgetId, newSpent)
        }
    }
    
    suspend fun recalculateBudgetSpending(month: Int, year: Int) {
        val budgets = budgetDao.getBudgetsForMonth(month, year)
        
        budgets.collect { budgetList ->
            for (budget in budgetList) {
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, 1, 0, 0, 0)
                val startTime = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endTime = calendar.timeInMillis
                
                val spent = transactionDao.getTransactionsByDateRangeAndType(
                    startTime, endTime, TransactionType.DEBIT
                ).filter { it.categoryId == budget.categoryId }
                 .sumOf { it.amount }
                
                budgetDao.updateBudgetSpent(budget.id, spent)
            }
        }
    }
    
    suspend fun getBudgetsNearLimit(): List<Budget> {
        return budgetDao.getBudgetsNearLimit()
    }
    
    suspend fun getExceededBudgets(): List<Budget> {
        return budgetDao.getExceededBudgets()
    }
    
    suspend fun generateBudgetSuggestions(month: Int, year: Int): Map<String, Double> {
        // Get previous 3 months data for suggestions
        val suggestions = mutableMapOf<String, Double>()
        
        for (i in 1..3) {
            val prevMonth = if (month - i <= 0) 12 + (month - i) else month - i
            val prevYear = if (month - i <= 0) year - 1 else year
            
            val calendar = Calendar.getInstance()
            calendar.set(prevYear, prevMonth - 1, 1, 0, 0, 0)
            val startTime = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endTime = calendar.timeInMillis
            
            val categorySpending = transactionDao.getCategoryWiseSpending(startTime, endTime)
            
            for (spending in categorySpending) {
                val currentSuggestion = suggestions[spending.categoryId] ?: 0.0
                suggestions[spending.categoryId] = maxOf(currentSuggestion, spending.totalAmount * 1.1) // 10% buffer
            }
        }
        
        return suggestions
    }
    
    suspend fun getTotalBudgetForMonth(month: Int, year: Int): Double {
        return budgetDao.getTotalBudgetForMonth(month, year) ?: 0.0
    }
    
    suspend fun getTotalSpentForMonth(month: Int, year: Int): Double {
        return budgetDao.getTotalSpentForMonth(month, year) ?: 0.0
    }
    
    suspend fun createBudgetsFromSuggestions(suggestions: Map<String, Double>, month: Int, year: Int) {
        for ((categoryId, suggestedAmount) in suggestions) {
            val existingBudget = getBudgetByCategoryAndMonth(categoryId, month, year)
            if (existingBudget == null) {
                createBudget(categoryId, suggestedAmount, month, year)
            }
        }
    }
    
    suspend fun getActiveBudgets(): List<Budget> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        
        return budgetDao.getBudgetsForMonth(currentMonth, currentYear).let { flow ->
            // Convert Flow to List for AlertsViewModel
            val budgets = mutableListOf<Budget>()
            flow.collect { budgets.addAll(it) }
            budgets
        }
    }
}
