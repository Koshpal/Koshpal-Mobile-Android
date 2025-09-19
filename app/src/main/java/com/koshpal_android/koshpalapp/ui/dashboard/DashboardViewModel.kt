package com.koshpal_android.koshpalapp.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.data.local.dao.CategoryDao
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.model.CategorySpendingData
import com.koshpal_android.koshpalapp.model.MerchantSpendingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {
    
    private val _currentMonth = MutableStateFlow(getCurrentMonthYear())
    val currentMonth: StateFlow<Pair<Int, Int>> = _currentMonth.asStateFlow()
    
    private val _monthlyData = MutableStateFlow(MonthlyDashboardData())
    val monthlyData: StateFlow<MonthlyDashboardData> = _monthlyData.asStateFlow()
    
    private val _categorySpending = MutableStateFlow<List<CategorySpendingData>>(emptyList())
    val categorySpending: StateFlow<List<CategorySpendingData>> = _categorySpending.asStateFlow()
    
    private val _topMerchants = MutableStateFlow<List<MerchantSpendingData>>(emptyList())
    val topMerchants: StateFlow<List<MerchantSpendingData>> = _topMerchants.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private fun getCurrentMonthYear(): Pair<Int, Int> {
        val calendar = Calendar.getInstance()
        return Pair(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }
    
    fun loadCurrentMonthData() {
        val (month, year) = _currentMonth.value
        loadMonthData(month, year)
    }
    
    fun navigateToPreviousMonth() {
        val (currentMonth, currentYear) = _currentMonth.value
        val newMonth = if (currentMonth == 1) 12 else currentMonth - 1
        val newYear = if (currentMonth == 1) currentYear - 1 else currentYear
        
        _currentMonth.value = Pair(newMonth, newYear)
        loadMonthData(newMonth, newYear)
    }
    
    fun navigateToNextMonth() {
        val (currentMonth, currentYear) = _currentMonth.value
        val newMonth = if (currentMonth == 12) 1 else currentMonth + 1
        val newYear = if (currentMonth == 12) currentYear + 1 else currentYear
        
        _currentMonth.value = Pair(newMonth, newYear)
        loadMonthData(newMonth, newYear)
    }
    
    private fun loadMonthData(month: Int, year: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Calculate date range for the month
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endTime = calendar.timeInMillis
                
                // Load basic monthly data
                val totalIncome = transactionRepository.getTotalAmountByTypeAndDateRange(
                    TransactionType.CREDIT, startTime, endTime
                )
                val totalExpense = transactionRepository.getTotalAmountByTypeAndDateRange(
                    TransactionType.DEBIT, startTime, endTime
                )
                val totalSavings = totalIncome - totalExpense
                
                _monthlyData.value = MonthlyDashboardData(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    totalSavings = totalSavings
                )
                
                // Load category-wise spending
                loadCategorySpending(startTime, endTime)
                
                // Load top merchants
                loadTopMerchants(startTime, endTime)
                
            } catch (e: Exception) {
                // Handle error
                _monthlyData.value = MonthlyDashboardData()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadCategorySpending(startTime: Long, endTime: Long) {
        try {
            val categorySpending = transactionRepository.getCategoryWiseSpending(startTime, endTime)
            val categorySpendingData = mutableListOf<CategorySpendingData>()
            
            for (spending in categorySpending) {
                val category = categoryDao.getCategoryById(spending.categoryId)
                if (category != null) {
                    categorySpendingData.add(
                        CategorySpendingData(
                            categoryId = category.id,
                            categoryName = category.name,
                            amount = spending.totalAmount,
                            color = category.color,
                            icon = category.icon,
                            transactionCount = getTransactionCountForCategory(
                                category.id, startTime, endTime
                            )
                        )
                    )
                }
            }
            
            // Sort by amount descending
            _categorySpending.value = categorySpendingData.sortedByDescending { it.amount }
            
        } catch (e: Exception) {
            _categorySpending.value = emptyList()
        }
    }
    
    private suspend fun loadTopMerchants(startTime: Long, endTime: Long) {
        try {
            val transactions = transactionRepository.getTransactionsByDateRangeAndType(
                startTime, endTime, TransactionType.DEBIT
            )
            
            val merchantSpending = transactions
                .groupBy { it.merchant }
                .map { (merchant, transactions) ->
                    MerchantSpendingData(
                        merchantName = merchant,
                        totalAmount = transactions.sumOf { it.amount },
                        transactionCount = transactions.size
                    )
                }
                .sortedByDescending { it.totalAmount }
                .take(10) // Top 10 merchants
            
            _topMerchants.value = merchantSpending
            
        } catch (e: Exception) {
            _topMerchants.value = emptyList()
        }
    }
    
    private suspend fun getTransactionCountForCategory(
        categoryId: String, 
        startTime: Long, 
        endTime: Long
    ): Int {
        return try {
            transactionRepository.getTransactionsByDateRangeAndType(
                startTime, endTime, TransactionType.DEBIT
            ).count { it.categoryId == categoryId }
        } catch (e: Exception) {
            0
        }
    }
}

data class MonthlyDashboardData(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalSavings: Double = 0.0
)
