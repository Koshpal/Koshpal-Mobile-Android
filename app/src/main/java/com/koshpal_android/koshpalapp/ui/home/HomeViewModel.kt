package com.koshpal_android.koshpalapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import com.koshpal_android.koshpalapp.repository.SavingsGoalRepository
import com.koshpal_android.koshpalapp.ui.home.model.HomeUiState
import com.koshpal_android.koshpalapp.ui.home.model.MonthlySpendingData
import com.koshpal_android.koshpalapp.ui.home.model.MonthYearOption
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions: StateFlow<List<Transaction>> = _recentTransactions.asStateFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Get all transactions
                transactionRepository.getAllTransactions().collect { allTransactions ->
                    
                    android.util.Log.d("HomeViewModel", "📊 Loading dashboard data...")
                    android.util.Log.d("HomeViewModel", "📊 Total transactions found: ${allTransactions.size}")
                    
                    // Calculate real financial data
                    var totalIncome = 0.0
                    var totalExpenses = 0.0
                    
                    for (transaction in allTransactions) {
                        if (transaction.type == TransactionType.CREDIT) {
                            totalIncome += transaction.amount
                        } else if (transaction.type == TransactionType.DEBIT) {
                            totalExpenses += transaction.amount
                        }
                    }
                    
                    val currentBalance = totalIncome - totalExpenses
                    
                    android.util.Log.d("HomeViewModel", "💰 Calculated - Income: ₹$totalIncome, Expenses: ₹$totalExpenses, Balance: ₹$currentBalance")
                    
                    // Calculate current month data
                    val currentState = _uiState.value
                    val selectedMonth = currentState.selectedMonth
                    val selectedYear = currentState.selectedYear
                    
                    android.util.Log.d("HomeViewModel", "📅 Filtering for selected month: ${selectedMonth + 1}/$selectedYear")
                    
                    var currentMonthIncome = 0.0
                    var currentMonthExpenses = 0.0
                    
                    val calendar = java.util.Calendar.getInstance()
                    for (transaction in allTransactions) {
                        calendar.timeInMillis = transaction.timestamp
                        val transactionMonth = calendar.get(java.util.Calendar.MONTH)
                        val transactionYear = calendar.get(java.util.Calendar.YEAR)
                        
                        if (transactionMonth == selectedMonth && transactionYear == selectedYear) {
                            if (transaction.type == TransactionType.CREDIT) {
                                currentMonthIncome += transaction.amount
                            } else if (transaction.type == TransactionType.DEBIT) {
                                currentMonthExpenses += transaction.amount
                            }
                        }
                    }
                    
                    val currentMonthBalance = currentMonthIncome - currentMonthExpenses
                    android.util.Log.d("HomeViewModel", "📅 Selected Month Data - Income: ₹$currentMonthIncome, Expenses: ₹$currentMonthExpenses, Balance: ₹$currentMonthBalance")
                    
                    android.util.Log.d("HomeViewModel", "🔄 Step 1: Getting available months...")
                    val availableMonths = getAvailableMonths(allTransactions)
                    android.util.Log.d("HomeViewModel", "✅ Step 1 completed: ${availableMonths.size} available months")
                    
                    android.util.Log.d("HomeViewModel", "🔄 Step 1b: Getting last 3 months data...")
                    // Get last 3 months data
                    val last3MonthsData = try {
                        getLast3MonthsData(allTransactions)
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "❌ Error in getLast3MonthsData: ${e.message}")
                        emptyList()
                    }
                    android.util.Log.d("HomeViewModel", "✅ Step 1 completed: ${last3MonthsData.size} months")
                    
                    android.util.Log.d("HomeViewModel", "🔄 Step 2: Getting recent transactions...")
                    // Load recent transactions (last 5)
                    val recentTransactions = try {
                        allTransactions
                            .sortedByDescending { it.timestamp }
                            .take(5)
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "❌ Error getting recent transactions: ${e.message}")
                        emptyList()
                    }
                    android.util.Log.d("HomeViewModel", "✅ Step 2 completed: ${recentTransactions.size} recent transactions")
                    
                    _recentTransactions.value = recentTransactions
                    
                    android.util.Log.d("HomeViewModel", "🔄 Step 3: Setting default budget data...")
                    // Skip problematic budget methods for now - use defaults
                    val budgetSpent = 0.0
                    val budgetLimit = 0.0
                    android.util.Log.d("HomeViewModel", "✅ Step 3 completed: budgetSpent=₹$budgetSpent, budgetLimit=₹$budgetLimit")
                    
                    android.util.Log.d("HomeViewModel", "🔄 Step 4: Creating new UI state...")
                    val newState = _uiState.value.copy(
                        isLoading = false,
                        currentBalance = currentBalance,
                        totalBalance = currentBalance,
                        totalIncome = totalIncome,
                        totalExpenses = totalExpenses,
                        budgetSpent = budgetSpent,
                        budgetLimit = budgetLimit,
                        hasTransactions = allTransactions.isNotEmpty(),
                        transactionCount = allTransactions.size,
                        last3MonthsData = last3MonthsData,
                        availableMonths = availableMonths,
                        currentMonthIncome = currentMonthIncome,
                        currentMonthExpenses = currentMonthExpenses,
                        currentMonthBalance = currentMonthBalance,
                        errorMessage = null
                    )
                    
                    android.util.Log.d("HomeViewModel", "🔄 Updating UI state:")
                    android.util.Log.d("HomeViewModel", "   hasTransactions: ${newState.hasTransactions}")
                    android.util.Log.d("HomeViewModel", "   totalIncome: ₹${newState.totalIncome}")
                    android.util.Log.d("HomeViewModel", "   totalExpenses: ₹${newState.totalExpenses}")
                    android.util.Log.d("HomeViewModel", "   currentBalance: ₹${newState.currentBalance}")
                    android.util.Log.d("HomeViewModel", "   transactionCount: ${newState.transactionCount}")
                    
                    // Force UI state update
                    _uiState.value = newState
                    android.util.Log.d("HomeViewModel", "✅ UI state updated successfully!")
                    
                    // Also emit to recent transactions
                    android.util.Log.d("HomeViewModel", "📱 Recent transactions count: ${recentTransactions.size}")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }

    fun selectMonth(month: Int, year: Int) {
        android.util.Log.d("HomeViewModel", "📅 Month selected: ${month + 1}/$year")
        _uiState.value = _uiState.value.copy(
            selectedMonth = month,
            selectedYear = year
        )
        // Reload data with new month selection
        loadDashboardData()
    }

    private fun getAvailableMonths(transactions: List<Transaction>): List<MonthYearOption> {
        val monthsSet = mutableSetOf<Pair<Int, Int>>()
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        
        // Add current month even if no transactions
        monthsSet.add(Pair(currentMonth, currentYear))
        
        // Add months from transactions
        for (transaction in transactions) {
            calendar.timeInMillis = transaction.timestamp
            val month = calendar.get(java.util.Calendar.MONTH)
            val year = calendar.get(java.util.Calendar.YEAR)
            monthsSet.add(Pair(month, year))
        }
        
        val monthFormat = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
        
        return monthsSet.map { (month, year) ->
            calendar.set(year, month, 1)
            MonthYearOption(
                month = month,
                year = year,
                displayName = monthFormat.format(calendar.time),
                isCurrentMonth = month == currentMonth && year == currentYear
            )
        }.sortedWith(compareByDescending<MonthYearOption> { it.year }.thenByDescending { it.month })
    }

    private fun getLast3MonthsData(transactions: List<Transaction>): List<MonthlySpendingData> {
        val calendar = Calendar.getInstance()
        val monthlyData = mutableListOf<MonthlySpendingData>()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        
        for (i in 0..2) {
            val monthStart = calendar.clone() as Calendar
            monthStart.add(Calendar.MONTH, -i)
            monthStart.set(Calendar.DAY_OF_MONTH, 1)
            monthStart.set(Calendar.HOUR_OF_DAY, 0)
            monthStart.set(Calendar.MINUTE, 0)
            monthStart.set(Calendar.SECOND, 0)
            
            val monthEnd = monthStart.clone() as Calendar
            monthEnd.add(Calendar.MONTH, 1)
            monthEnd.add(Calendar.MILLISECOND, -1)
            
            val monthTransactions = transactions.filter { transaction ->
                transaction.timestamp >= monthStart.timeInMillis && transaction.timestamp <= monthEnd.timeInMillis
            }
            
            val monthIncome = monthTransactions
                .filter { it.type == TransactionType.CREDIT }
                .sumOf { it.amount }
            
            val monthExpenses = monthTransactions
                .filter { it.type == TransactionType.DEBIT }
                .sumOf { it.amount }
            
            monthlyData.add(
                MonthlySpendingData(
                    month = monthFormat.format(monthStart.time),
                    year = monthStart.get(Calendar.YEAR),
                    totalSpent = monthExpenses,
                    totalIncome = monthIncome,
                    transactionCount = monthTransactions.size
                )
            )
        }
        
        return monthlyData.reversed() // Show oldest to newest
    }
    
    private fun getCurrentMonthExpenses(transactions: List<Transaction>): Double {
        val calendar = Calendar.getInstance()
        val monthStart = calendar.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        monthStart.set(Calendar.HOUR_OF_DAY, 0)
        monthStart.set(Calendar.MINUTE, 0)
        monthStart.set(Calendar.SECOND, 0)
        
        return transactions
            .filter { it.timestamp >= monthStart.timeInMillis && it.type == TransactionType.DEBIT }
            .sumOf { it.amount }
    }
    
    private suspend fun getBudgetLimit(): Double {
        return try {
            val budgets = budgetRepository.getAllActiveBudgets()
            var totalLimit = 0.0
            budgets.collect { budgetList ->
                totalLimit = budgetList.sumOf { it.monthlyLimit }
            }
            if (totalLimit > 0) totalLimit else 20000.0 // Default budget
        } catch (e: Exception) {
            20000.0 // Default budget if no budgets exist
        }
    }

    private suspend fun calculateFinancialHealthScore(): Int {
        return try {
            // Simple calculation based on spending vs income ratio
            val monthlyIncome = transactionRepository.getMonthlyIncome()
            val monthlyExpenses = transactionRepository.getMonthlyExpenses()
            
            when {
                monthlyIncome <= 0 -> 50 // Default score if no income data
                monthlyExpenses <= 0 -> 100 // Perfect score if no expenses
                else -> {
                    val ratio = monthlyExpenses / monthlyIncome
                    when {
                        ratio <= 0.3 -> 95 // Excellent
                        ratio <= 0.5 -> 85 // Very Good
                        ratio <= 0.7 -> 70 // Good
                        ratio <= 0.9 -> 55 // Fair
                        else -> 30 // Poor
                    }
                }
            }
        } catch (e: Exception) {
            75 // Default score on error
        }
    }

    private suspend fun getMonthlySpending(): Double {
        return try {
            transactionRepository.getMonthlyExpenses()
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun getActiveSavingsGoalsCount(): Int {
        return try {
            savingsGoalRepository.getActiveSavingsGoalsCount()
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun loadRecentTransactions() {
        try {
            val transactions = transactionRepository.getRecentTransactions(5)
            _recentTransactions.value = transactions
            
            // Debug: Log transaction count
            android.util.Log.d("HomeViewModel", "Loaded ${transactions.size} recent transactions")
        } catch (e: Exception) {
            _recentTransactions.value = emptyList()
            android.util.Log.e("HomeViewModel", "Error loading transactions", e)
        }
    }

    private fun getHealthStatus(score: Int): String {
        return when {
            score >= 90 -> "Excellent"
            score >= 75 -> "Very Good"
            score >= 60 -> "Good"
            score >= 45 -> "Fair"
            else -> "Needs Improvement"
        }
    }

    fun onPermissionsGranted() {
        viewModelScope.launch {
            // Start SMS processing service
            // This would trigger the transaction processing service
            refreshData()
        }
    }

    fun onTransactionClick(transaction: Transaction) {
        // Handle transaction click - could open categorization dialog
        viewModelScope.launch {
            // Navigate to transaction details or categorization
        }
    }

    fun onAddTransactionClick() {
        // Handle add transaction click
        viewModelScope.launch {
            // Open add transaction dialog or screen
        }
    }

    fun onCategorizationClick() {
        // Handle categorization feature click
        viewModelScope.launch {
            // Open categorization screen
        }
    }

    fun refreshData() {
        loadDashboardData()
    }
    
    suspend fun getTransactionCount(): Int {
        return try {
            transactionRepository.getTransactionCount()
        } catch (e: Exception) {
            0
        }
    }
}