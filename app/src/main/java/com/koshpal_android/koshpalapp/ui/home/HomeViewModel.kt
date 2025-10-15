package com.koshpal_android.koshpalapp.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.repository.TransactionRepository
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
    application: Application,
    private val transactionRepository: TransactionRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recentTransactions: StateFlow<List<Transaction>> = _recentTransactions.asStateFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    init {
        viewModelScope.launch {
            loadDashboardData()
        }
    }

    private suspend fun loadDashboardData() {
        android.util.Log.d("HomeViewModel", "📊 ===== LOADING DASHBOARD DATA =====")
        
        // Set loading state to true at the start
        _uiState.value = _uiState.value.copy(isLoading = true)
        android.util.Log.d("HomeViewModel", "⏳ Loading state: TRUE - Shimmer should be visible")
        
        try {
            // CRITICAL FIX: Use the same database instance as DebugDataManager
            val context = getApplication<Application>().applicationContext
            val database = com.koshpal_android.koshpalapp.data.local.KoshpalDatabase.getDatabase(context)
            val allTransactions = database.transactionDao().getAllTransactionsOnce()
            android.util.Log.d("HomeViewModel", "📊 Total transactions found: ${allTransactions.size}")
            
            // Debug: Log each transaction found
            allTransactions.forEachIndexed { index, transaction ->
                android.util.Log.d("HomeViewModel", "   ${index + 1}. ${transaction.merchant} - ₹${transaction.amount} (${transaction.type})")
            }
            
            // Calculate totals with detailed logging
            var totalIncome = 0.0
            var totalExpenses = 0.0
            
            allTransactions.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.CREDIT -> {
                        totalIncome += transaction.amount
                        android.util.Log.d("HomeViewModel", "   ➕ Income: ${transaction.merchant} +₹${transaction.amount}")
                    }
                    TransactionType.DEBIT -> {
                        totalExpenses += transaction.amount
                        android.util.Log.d("HomeViewModel", "   ➖ Expense: ${transaction.merchant} -₹${transaction.amount}")
                    }
                    TransactionType.TRANSFER -> {
                        // FIXED: Treat transfers as expenses (money going out)
                        totalExpenses += transaction.amount
                        android.util.Log.d("HomeViewModel", "   🔄 Transfer (counted as expense): ${transaction.merchant} -₹${transaction.amount}")
                    }
                }
            }
            
            val currentBalance = totalIncome - totalExpenses
            android.util.Log.d("HomeViewModel", "💰 CALCULATED TOTALS - Income: ₹$totalIncome, Expenses: ₹$totalExpenses, Balance: ₹$currentBalance")
            
            // CRITICAL CHANGE: Show CURRENT MONTH data instead of total
            android.util.Log.d("HomeViewModel", "📅 Calculating current month data...")
            
            val calendar = java.util.Calendar.getInstance()
            val currentMonth = calendar.get(java.util.Calendar.MONTH)
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            
            android.util.Log.d("HomeViewModel", "📅 Current month: ${currentMonth + 1}/$currentYear")
            
            var currentMonthIncome = 0.0
            var currentMonthExpenses = 0.0
            
            allTransactions.forEach { transaction ->
                calendar.timeInMillis = transaction.timestamp
                val transactionMonth = calendar.get(java.util.Calendar.MONTH)
                val transactionYear = calendar.get(java.util.Calendar.YEAR)
                
                android.util.Log.d("HomeViewModel", "🔍 Transaction: ${transaction.merchant} - Date: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp))} - Month: ${transactionMonth + 1}/$transactionYear")
                
                if (transactionMonth == currentMonth && transactionYear == currentYear) {
                    when (transaction.type) {
                        TransactionType.CREDIT -> {
                            currentMonthIncome += transaction.amount
                            android.util.Log.d("HomeViewModel", "   ✅ Current Month Income: ${transaction.merchant} +₹${transaction.amount}")
                        }
                        TransactionType.DEBIT -> {
                            currentMonthExpenses += transaction.amount
                            android.util.Log.d("HomeViewModel", "   ✅ Current Month Expense: ${transaction.merchant} -₹${transaction.amount}")
                        }
                        TransactionType.TRANSFER -> {
                            // FIXED: Treat transfers as expenses (money going out)
                            currentMonthExpenses += transaction.amount
                            android.util.Log.d("HomeViewModel", "   ✅ Current Month Transfer (counted as expense): ${transaction.merchant} -₹${transaction.amount}")
                        }
                    }
                } else {
                    android.util.Log.d("HomeViewModel", "   ❌ Transaction NOT in current month: ${transaction.merchant} (${transactionMonth + 1}/$transactionYear vs ${currentMonth + 1}/$currentYear)")
                }
            }
            
            val currentMonthBalance = currentMonthIncome - currentMonthExpenses
            
            // CRITICAL: If no current month data, show total data as fallback
            val hasCurrentMonthData = currentMonthIncome > 0 || currentMonthExpenses > 0
            
            val displayIncome = if (hasCurrentMonthData) currentMonthIncome else totalIncome
            val displayExpenses = if (hasCurrentMonthData) currentMonthExpenses else totalExpenses
            val displayBalance = if (hasCurrentMonthData) currentMonthBalance else (totalIncome - totalExpenses)
            
            android.util.Log.d("HomeViewModel", "📊 DISPLAY DECISION:")
            android.util.Log.d("HomeViewModel", "   Has current month data: $hasCurrentMonthData")
            android.util.Log.d("HomeViewModel", "   Using ${if (hasCurrentMonthData) "CURRENT MONTH" else "TOTAL"} data for display")
            
            android.util.Log.d("HomeViewModel", "📅 CURRENT MONTH DATA:")
            android.util.Log.d("HomeViewModel", "   Income: ₹$displayIncome")
            android.util.Log.d("HomeViewModel", "   Expenses: ₹$displayExpenses")
            android.util.Log.d("HomeViewModel", "   Balance: ₹$displayBalance")
            android.util.Log.d("HomeViewModel", "📊 TOTAL DATA (for reference):")
            android.util.Log.d("HomeViewModel", "   Total Income: ₹$totalIncome")
            android.util.Log.d("HomeViewModel", "   Total Expenses: ₹$totalExpenses")
            android.util.Log.d("HomeViewModel", "   Total Balance: ₹${totalIncome - totalExpenses}")
            
            android.util.Log.d("HomeViewModel", "🔄 Creating simplified UI state...")
            
            // Get recent transactions (last 10)
            val recentTransactions = allTransactions
                .sortedByDescending { it.timestamp }
                .take(10)
            
            // Update recent transactions
            _recentTransactions.value = recentTransactions
            
            // Prepare monthly datasets
            val availableMonths = emptyList<com.koshpal_android.koshpalapp.ui.home.model.MonthYearOption>()
            val last3MonthsData = emptyList<com.koshpal_android.koshpalapp.ui.home.model.MonthlySpendingData>()
            val last4 = transactionRepository.getLastNMonthsIncomeExpenses(4).map { (label, pair) ->
                com.koshpal_android.koshpalapp.ui.home.model.MonthlySpendingData(
                    month = label,
                    year = Calendar.getInstance().get(Calendar.YEAR),
                    totalSpent = pair.second,
                    totalIncome = pair.first,
                    transactionCount = 0
                )
            }
            
            val budgetSpent = 0.0
            val budgetLimit = 0.0
            
            // Generate daily spending data for current month
            val dailySpendingData = generateDailySpendingForCurrentMonth(allTransactions)
            
            // Create new UI state
            val newState = _uiState.value.copy(
                isLoading = false,
                currentBalance = displayBalance,
                totalBalance = displayBalance,
                totalIncome = displayIncome,
                totalExpenses = displayExpenses,
                budgetSpent = budgetSpent,
                budgetLimit = budgetLimit,
                hasTransactions = allTransactions.isNotEmpty(),
                transactionCount = allTransactions.size,
                last3MonthsData = last3MonthsData,
                availableMonths = availableMonths,
                last4MonthsComparison = last4,
                currentMonthIncome = displayIncome,
                currentMonthExpenses = displayExpenses,
                currentMonthBalance = displayBalance,
                dailySpendingData = dailySpendingData,
                errorMessage = null
            )
            
            android.util.Log.d("HomeViewModel", "🔄 CREATING NEW STATE:")
            android.util.Log.d("HomeViewModel", "   hasTransactions: ${newState.hasTransactions}")
            android.util.Log.d("HomeViewModel", "   transactionCount: ${newState.transactionCount}")
            android.util.Log.d("HomeViewModel", "   currentBalance: ₹${newState.currentBalance}")
            android.util.Log.d("HomeViewModel", "   totalIncome: ₹${newState.totalIncome}")
            android.util.Log.d("HomeViewModel", "   totalExpenses: ₹${newState.totalExpenses}")
            
            // Apply the new state
            _uiState.value = newState
            android.util.Log.d("HomeViewModel", "✅ NEW STATE APPLIED TO UI!")
            android.util.Log.d("HomeViewModel", "📱 Recent transactions count: ${recentTransactions.size}")
            android.util.Log.d("HomeViewModel", "✅ Loading state: FALSE - Shimmer should hide now")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "❌ CRITICAL ERROR loading dashboard data: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Failed to load data: ${e.message}"
            )
            android.util.Log.d("HomeViewModel", "❌ Loading state: FALSE (error) - Shimmer should hide")
        }
    }

    fun selectMonth(month: Int, year: Int) {
        android.util.Log.d("HomeViewModel", "📅 Month selected: ${month + 1}/$year")
        _uiState.value = _uiState.value.copy(
            selectedMonth = month,
            selectedYear = year
        )
        // Reload data with new month selection
        viewModelScope.launch {
            loadDashboardData()
        }
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
        return 20000.0 // Default budget limit
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
        return 0 // No savings goals in simplified version
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
        android.util.Log.d("HomeViewModel", "🔄 Manual refresh triggered")
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            loadDashboardData()
        }
    }
    
    fun forceRefreshNow() {
        android.util.Log.d("HomeViewModel", "🚀 Force refresh NOW triggered")
        viewModelScope.launch {
            loadDashboardData()
        }
    }
    
    suspend fun getTransactionCount(): Int {
        return try {
            transactionRepository.getTransactionCount()
        } catch (e: Exception) {
            0
        }
    }
    
    private fun generateDailySpendingForCurrentMonth(allTransactions: List<Transaction>): List<com.koshpal_android.koshpalapp.ui.home.model.DailySpendingData> {
        val calendar = java.util.Calendar.getInstance()
        val currentMonth = calendar.get(java.util.Calendar.MONTH)
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        
        // Get first day of current month
        calendar.set(currentYear, currentMonth, 1, 0, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val firstDayOfMonth = calendar.timeInMillis
        
        // Get last day of current month
        calendar.add(java.util.Calendar.MONTH, 1)
        calendar.add(java.util.Calendar.MILLISECOND, -1)
        val lastDayOfMonth = calendar.timeInMillis
        
        // Filter transactions for current month (both expenses and income)
        val currentMonthTransactions = allTransactions.filter { transaction ->
            transaction.timestamp >= firstDayOfMonth && 
            transaction.timestamp <= lastDayOfMonth
        }
        
        // Get number of days in current month
        calendar.set(currentYear, currentMonth, 1)
        val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        
        val dailyData = mutableListOf<com.koshpal_android.koshpalapp.ui.home.model.DailySpendingData>()
        
        // Generate data for each day of the month
        for (day in 1..daysInMonth) {
            val dayStart = java.util.Calendar.getInstance().apply {
                set(currentYear, currentMonth, day, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val dayEnd = java.util.Calendar.getInstance().apply {
                set(currentYear, currentMonth, day, 23, 59, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }.timeInMillis
            
            val dayTransactions = currentMonthTransactions.filter { transaction ->
                transaction.timestamp >= dayStart && transaction.timestamp <= dayEnd
            }
            
            var totalSpent = 0.0
            var totalIncome = 0.0
            
            dayTransactions.forEach { transaction ->
                when (transaction.type) {
                    TransactionType.CREDIT -> totalIncome += transaction.amount
                    TransactionType.DEBIT, TransactionType.TRANSFER -> totalSpent += transaction.amount
                }
            }
            
            val dayLabel = if (day % 5 == 0 || day == 1 || day == daysInMonth) day.toString() else ""
            
            dailyData.add(
                com.koshpal_android.koshpalapp.ui.home.model.DailySpendingData(
                    day = day,
                    dayLabel = dayLabel,
                    totalSpent = totalSpent,
                    totalIncome = totalIncome
                )
            )
        }
        
        android.util.Log.d("HomeViewModel", "📊 Generated ${dailyData.size} days of spending data for current month")
        return dailyData
    }
    
}