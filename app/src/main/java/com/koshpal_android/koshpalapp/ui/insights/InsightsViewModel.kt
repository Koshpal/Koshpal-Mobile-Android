package com.koshpal_android.koshpalapp.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.data.local.dao.FinancialInsightDao
import com.koshpal_android.koshpalapp.model.FinancialInsight
import com.koshpal_android.koshpalapp.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val financialInsightDao: FinancialInsightDao
) : ViewModel() {
    
    private val _currentInsight = MutableStateFlow<FinancialInsight?>(null)
    val currentInsight: StateFlow<FinancialInsight?> = _currentInsight.asStateFlow()
    
    private val _trendData = MutableStateFlow<List<MonthlyTrend>>(emptyList())
    val trendData: StateFlow<List<MonthlyTrend>> = _trendData.asStateFlow()
    
    private val _recommendations = MutableStateFlow<List<Recommendation>>(emptyList())
    val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()
    
    private val _monthlyComparison = MutableStateFlow(MonthlyComparison())
    val monthlyComparison: StateFlow<MonthlyComparison> = _monthlyComparison.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadFinancialInsights() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)
                
                // Load or generate current month insight
                loadCurrentMonthInsight(currentMonth, currentYear)
                
                // Load 6-month trend data
                loadTrendData(currentMonth, currentYear)
                
                // Load monthly comparison
                loadMonthlyComparison(currentMonth, currentYear)
                
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadCurrentMonthInsight(month: Int, year: Int) {
        try {
            var insight = financialInsightDao.getInsightByMonth(month, year)
            
            if (insight == null) {
                // Generate new insight
                insight = generateFinancialInsight(month, year)
                financialInsightDao.insertInsight(insight)
            }
            
            _currentInsight.value = insight
            
            // Generate recommendations based on insight
            generateRecommendations(insight)
            
        } catch (e: Exception) {
            _currentInsight.value = null
        }
    }
    
    private suspend fun generateFinancialInsight(month: Int, year: Int): FinancialInsight {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        val startTime = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endTime = calendar.timeInMillis
        
        val totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
            TransactionType.CREDIT, startTime, endTime
        )
        val totalExpense = transactionRepository.getTotalAmountByTypeAndDateRange(
            TransactionType.DEBIT, startTime, endTime
        )
        
        val savingsRate = if (totalIncome > 0) {
            ((totalIncome - totalExpense) / totalIncome * 100).toFloat()
        } else 0f
        
        val expenseRatio = if (totalIncome > 0) {
            (totalExpense / totalIncome * 100).toFloat()
        } else 100f
        
        val healthScore = calculateHealthScore(savingsRate, expenseRatio)
        val recommendations = generateSmartRecommendations(savingsRate, expenseRatio, totalExpense)
        
        // Get top spending category
        val categorySpending = transactionRepository.getCategoryWiseSpending(startTime, endTime)
        val topSpendingCategory = categorySpending.maxByOrNull { it.totalAmount }?.categoryId ?: "others"
        
        // Calculate growth rates (simplified)
        val expenseGrowth = calculateExpenseGrowth(month, year, totalExpense)
        val incomeGrowth = calculateIncomeGrowth(month, year, totalIncome)
        
        return FinancialInsight(
            id = UUID.randomUUID().toString(),
            month = month,
            year = year,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savingsRate = savingsRate,
            healthScore = healthScore,
            recommendations = recommendations,
            topSpendingCategory = topSpendingCategory,
            expenseGrowth = expenseGrowth,
            incomeGrowth = incomeGrowth
        )
    }
    
    private fun calculateHealthScore(savingsRate: Float, expenseRatio: Float): Int {
        var score = 50 // Base score
        
        // Savings rate contribution (40 points max)
        score += when {
            savingsRate >= 30 -> 40
            savingsRate >= 20 -> 30
            savingsRate >= 10 -> 20
            savingsRate >= 5 -> 10
            else -> 0
        }
        
        // Expense ratio contribution (30 points max)
        score += when {
            expenseRatio <= 50 -> 30
            expenseRatio <= 70 -> 20
            expenseRatio <= 80 -> 10
            expenseRatio <= 90 -> 5
            else -> 0
        }
        
        // Emergency fund factor (20 points max) - simplified
        score += 10 // Assume some emergency fund exists
        
        return minOf(100, maxOf(0, score))
    }
    
    private fun generateSmartRecommendations(
        savingsRate: Float,
        expenseRatio: Float,
        totalExpense: Double
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (savingsRate < 10) {
            recommendations.add("Try to save at least 10% of your income each month")
        }
        
        if (expenseRatio > 80) {
            recommendations.add("Your expenses are quite high. Consider reviewing your spending habits")
        }
        
        if (totalExpense > 50000) {
            recommendations.add("Consider setting up category-wise budgets to track your spending better")
        }
        
        recommendations.add("Set up automatic transfers to your savings account")
        recommendations.add("Review and cancel unused subscriptions")
        
        return recommendations
    }
    
    private suspend fun calculateExpenseGrowth(month: Int, year: Int, currentExpense: Double): Float {
        return try {
            val prevMonth = if (month == 1) 12 else month - 1
            val prevYear = if (month == 1) year - 1 else year
            
            val prevExpense = transactionRepository.getMonthlyExpense(prevMonth, prevYear)
            
            if (prevExpense > 0) {
                ((currentExpense - prevExpense) / prevExpense * 100).toFloat()
            } else 0f
        } catch (e: Exception) {
            0f
        }
    }
    
    private suspend fun calculateIncomeGrowth(month: Int, year: Int, currentIncome: Double): Float {
        return try {
            val prevMonth = if (month == 1) 12 else month - 1
            val prevYear = if (month == 1) year - 1 else year
            
            val prevIncome = transactionRepository.getMonthlyIncome(prevMonth, prevYear)
            
            if (prevIncome > 0) {
                ((currentIncome - prevIncome) / prevIncome * 100).toFloat()
            } else 0f
        } catch (e: Exception) {
            0f
        }
    }
    
    private suspend fun loadTrendData(currentMonth: Int, currentYear: Int) {
        try {
            val trends = mutableListOf<MonthlyTrend>()
            
            for (i in 5 downTo 0) {
                val calendar = Calendar.getInstance()
                calendar.set(currentYear, currentMonth - 1, 1)
                calendar.add(Calendar.MONTH, -i)
                
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                
                val income = transactionRepository.getMonthlyIncome(month, year)
                val expense = transactionRepository.getMonthlyExpense(month, year)
                
                trends.add(MonthlyTrend(month, year, income, expense))
            }
            
            _trendData.value = trends
        } catch (e: Exception) {
            _trendData.value = emptyList()
        }
    }
    
    private suspend fun loadMonthlyComparison(currentMonth: Int, currentYear: Int) {
        try {
            val thisMonthExpense = transactionRepository.getMonthlyExpense(currentMonth, currentYear)
            
            val prevMonth = if (currentMonth == 1) 12 else currentMonth - 1
            val prevYear = if (currentMonth == 1) currentYear - 1 else currentYear
            val lastMonthExpense = transactionRepository.getMonthlyExpense(prevMonth, prevYear)
            
            _monthlyComparison.value = MonthlyComparison(thisMonthExpense, lastMonthExpense)
        } catch (e: Exception) {
            _monthlyComparison.value = MonthlyComparison()
        }
    }
    
    private fun generateRecommendations(insight: FinancialInsight) {
        val recommendations = insight.recommendations.map { text ->
            Recommendation(
                id = UUID.randomUUID().toString(),
                title = getRecommendationTitle(text),
                description = text,
                priority = getRecommendationPriority(text),
                category = getRecommendationCategory(text)
            )
        }
        
        _recommendations.value = recommendations
    }
    
    private fun getRecommendationTitle(text: String): String {
        return when {
            text.contains("save") -> "💰 Increase Savings"
            text.contains("expenses") -> "📉 Reduce Expenses"
            text.contains("budget") -> "📊 Set Up Budgets"
            text.contains("automatic") -> "🔄 Automate Savings"
            text.contains("subscription") -> "📱 Review Subscriptions"
            else -> "💡 Financial Tip"
        }
    }
    
    private fun getRecommendationPriority(text: String): RecommendationPriority {
        return when {
            text.contains("at least") || text.contains("quite high") -> RecommendationPriority.HIGH
            text.contains("consider") -> RecommendationPriority.MEDIUM
            else -> RecommendationPriority.LOW
        }
    }
    
    private fun getRecommendationCategory(text: String): RecommendationCategory {
        return when {
            text.contains("save") -> RecommendationCategory.SAVINGS
            text.contains("expenses") -> RecommendationCategory.SPENDING
            text.contains("budget") -> RecommendationCategory.BUDGETING
            else -> RecommendationCategory.GENERAL
        }
    }
}

data class MonthlyTrend(
    val month: Int,
    val year: Int,
    val income: Double,
    val expense: Double
)

data class MonthlyComparison(
    val thisMonth: Double = 0.0,
    val lastMonth: Double = 0.0
)

data class Recommendation(
    val id: String,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val category: RecommendationCategory
)

enum class RecommendationPriority {
    HIGH, MEDIUM, LOW
}

enum class RecommendationCategory {
    SAVINGS, SPENDING, BUDGETING, GENERAL
}
