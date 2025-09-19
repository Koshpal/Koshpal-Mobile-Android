package com.koshpal_android.koshpalapp.ui.home.model

data class HomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "Hi, Chaitany",
    val currentBalance: Double = 0.0,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val budgetSpent: Double = 0.0,
    val budgetLimit: Double = 0.0,
    val hasTransactions: Boolean = false,
    val transactionCount: Int = 0,
    val last3MonthsData: List<MonthlySpendingData> = emptyList(),
    val errorMessage: String? = null,
    val hasPermissions: Boolean = false,
    // Month selection fields
    val selectedMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
    val selectedYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val availableMonths: List<MonthYearOption> = emptyList(),
    val currentMonthIncome: Double = 0.0,
    val currentMonthExpenses: Double = 0.0,
    val currentMonthBalance: Double = 0.0
)

data class MonthlySpendingData(
    val month: String,
    val year: Int,
    val totalSpent: Double,
    val totalIncome: Double,
    val transactionCount: Int
)

data class MonthYearOption(
    val month: Int,
    val year: Int,
    val displayName: String,
    val isCurrentMonth: Boolean = false
)
