package com.koshpal_android.koshpalapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import com.koshpal_android.koshpalapp.ui.budget.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.random.Random

/**
 * Revolutionary AdvancedBudgetViewModel powering the bulletproof budgeting system
 */
@HiltViewModel
class AdvancedBudgetViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdvancedBudgetUiState())
    val uiState: StateFlow<AdvancedBudgetUiState> = _uiState.asStateFlow()

    init {
        loadAdvancedBudgetData()
    }

    private fun loadAdvancedBudgetData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Collect all data streams
                combine(
                    transactionRepository.getAllTransactions(),
                    budgetRepository.getAllBudgets(),
                    flowOf(getCurrentMonthProgress())
                ) { transactions, budgets, monthProgress ->
                    Triple(transactions, budgets, monthProgress)
                }.collect { (transactions, budgets, monthProgress) ->
                    
                    // Process all advanced features
                    val processedData = processAdvancedBudgetData(transactions, budgets, monthProgress)
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        totalBudget = processedData.totalBudget,
                        totalSpent = processedData.totalSpent,
                        remainingBudget = processedData.remainingBudget,
                        monthProgress = processedData.monthProgress,
                        budgetHealthScore = processedData.healthScore,
                        tierBreakdown = processedData.tierBreakdown,
                        categories = processedData.categories,
                        universeData = processedData.universeData,
                        insights = processedData.insights,
                        predictiveAlerts = processedData.predictiveAlerts,
                        detectedSubscriptions = processedData.subscriptions,
                        upcomingPayments = processedData.upcomingPayments,
                        errorMessage = null
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load budget data: ${e.message}"
                )
            }
        }
    }

    private suspend fun processAdvancedBudgetData(
        transactions: List<Transaction>,
        budgets: List<com.koshpal_android.koshpalapp.model.Budget>,
        monthProgress: Float
    ): ProcessedBudgetData {
        
        // Calculate basic metrics
        val currentMonthTransactions = getCurrentMonthTransactions(transactions)
        val totalSpent = currentMonthTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val totalBudget = budgets.sumOf { it.amount }
        val remainingBudget = totalBudget - totalSpent
        val healthScore = calculateBudgetHealthScore(totalSpent, totalBudget, monthProgress)
        
        // Create tiered budget categories
        val categories = createTieredCategories(budgets, currentMonthTransactions)
        val tierBreakdown = createTierBreakdown(categories)
        
        // Generate Financial Universe data
        val universeData = createFinancialUniverseData(categories)
        
        // Generate Smart Insights (simplified for now)
        val insights = emptyList<SmartInsight>()
        
        // Generate Predictive Alerts (simplified for now)
        val predictiveAlerts = emptyList<PredictiveAlert>()
        
        // Detect Subscriptions (simplified for now)
        val subscriptions = emptyList<DetectedSubscription>()
        val upcomingPayments = emptyList<UpcomingPayment>()
        
        return ProcessedBudgetData(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remainingBudget = remainingBudget,
            monthProgress = monthProgress,
            healthScore = healthScore,
            tierBreakdown = tierBreakdown,
            categories = categories,
            universeData = universeData,
            insights = insights,
            predictiveAlerts = predictiveAlerts,
            subscriptions = subscriptions,
            upcomingPayments = upcomingPayments
        )
    }

    private fun getCurrentMonthTransactions(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        return transactions.filter { transaction ->
            calendar.timeInMillis = transaction.timestamp
            calendar.get(Calendar.MONTH) == currentMonth && 
            calendar.get(Calendar.YEAR) == currentYear
        }
    }

    private fun calculateBudgetHealthScore(spent: Double, budget: Double, monthProgress: Float): Float {
        if (budget <= 0) return 0f
        
        val spentRatio = (spent / budget).toFloat()
        val expectedSpentRatio = monthProgress
        
        return when {
            spentRatio <= expectedSpentRatio * 0.8f -> 1.0f // Excellent
            spentRatio <= expectedSpentRatio -> 0.8f // Good
            spentRatio <= expectedSpentRatio * 1.2f -> 0.6f // Fair
            spentRatio <= 1.0f -> 0.4f // Poor
            else -> 0.2f // Critical
        }.coerceIn(0f, 1f)
    }

    private fun createTieredCategories(
        budgets: List<com.koshpal_android.koshpalapp.model.Budget>,
        transactions: List<Transaction>
    ): List<BudgetCategory> {
        return budgets.map { budget ->
            val categoryTransactions = transactions.filter { it.category == budget.category }
            val spentAmount = categoryTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
            
            // Assign tier based on category type
            val tier = assignTierToCategory(budget.category)
            
            BudgetCategory(
                id = budget.id,
                name = budget.category,
                tier = tier,
                allocatedAmount = budget.amount,
                spentAmount = spentAmount,
                isRecurring = isRecurringCategory(budget.category),
                isAutoDetected = false
            )
        }
    }

    private fun assignTierToCategory(categoryName: String): BudgetTier {
        return when (categoryName.lowercase()) {
            "rent", "groceries", "utilities", "transport", "insurance", "phone" -> BudgetTier.ESSENTIALS
            "entertainment", "dining", "shopping", "hobbies", "travel" -> BudgetTier.WANTS
            "savings", "investment", "emergency fund", "debt repayment" -> BudgetTier.GOALS
            else -> BudgetTier.WANTS // Default to wants
        }
    }

    private fun isRecurringCategory(categoryName: String): Boolean {
        val recurringCategories = listOf("rent", "utilities", "insurance", "phone", "subscriptions")
        return recurringCategories.any { categoryName.lowercase().contains(it) }
    }

    private fun createTierBreakdown(categories: List<BudgetCategory>): Map<BudgetTier, TierData> {
        return BudgetTier.values().associateWith { tier ->
            val tierCategories = categories.filter { it.tier == tier }
            val allocatedAmount = tierCategories.sumOf { it.allocatedAmount }
            val spentAmount = tierCategories.sumOf { it.spentAmount }
            val healthScore = if (allocatedAmount > 0) {
                1f - (spentAmount / allocatedAmount).toFloat().coerceIn(0f, 1f)
            } else 1f
            
            TierData(
                tier = tier,
                allocatedAmount = allocatedAmount,
                spentAmount = spentAmount,
                categories = tierCategories,
                healthScore = healthScore,
                trend = TrendDirection.STABLE // Simplified for now
            )
        }
    }

    // Removed calculateTrend method - simplified in createTierBreakdown

    private fun createFinancialUniverseData(categories: List<BudgetCategory>): FinancialUniverseData {
        val planets = categories.mapIndexed { index, category ->
            BudgetPlanet(
                category = category,
                size = (20f + (category.allocatedAmount / 1000f).toFloat()).coerceIn(15f, 50f),
                orbitRadius = 80f + (index * 50f),
                orbitSpeed = 0.5f + Random.nextFloat() * 0.5f,
                currentAngle = Random.nextFloat() * 2f * Math.PI.toFloat(),
                pulseIntensity = if (category.spentPercentage > 0.8f) 0.8f else 0.0f
            )
        }
        
        return FinancialUniverseData(
            planets = planets,
            orbitAnimationSpeed = 1.0f
        )
    }

    // Removed generatePredictiveAlerts method - simplified for now

    // Removed generateUpcomingPayments method - simplified for now

    private fun getCurrentMonthProgress(): Float {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return dayOfMonth.toFloat() / daysInMonth.toFloat()
    }

    // Simplified methods for now
    fun exitScenarioMode() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            activeScenario = null,
            isScenarioMode = false
        )
    }

    fun addQuickExpense(categoryId: String, amount: Double, description: String) {
        viewModelScope.launch {
            try {
                // Create and save transaction
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = TransactionType.DEBIT,
                    category = getCategoryName(categoryId),
                    description = description,
                    timestamp = System.currentTimeMillis(),
                    merchant = "Manual Entry",
                    categoryId = categoryId,
                    confidence = 1.0f,
                    smsBody = ""
                )
                
                transactionRepository.insertTransaction(transaction)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add expense: ${e.message}"
                )
            }
        }
    }

    private fun getCategoryName(categoryId: String): String {
        return _uiState.value.categories.find { it.id == categoryId }?.name ?: "Other"
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // Data class for processed budget data
    private data class ProcessedBudgetData(
        val totalBudget: Double,
        val totalSpent: Double,
        val remainingBudget: Double,
        val monthProgress: Float,
        val healthScore: Float,
        val tierBreakdown: Map<BudgetTier, TierData>,
        val categories: List<BudgetCategory>,
        val universeData: FinancialUniverseData,
        val insights: List<SmartInsight>,
        val predictiveAlerts: List<PredictiveAlert>,
        val subscriptions: List<DetectedSubscription>,
        val upcomingPayments: List<UpcomingPayment>
    )
}
