package com.koshpal_android.koshpalapp.ui.categories.compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.BudgetCategory
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategoriesUiState(
    val isLoading: Boolean = false,
    val categorySpending: List<CategorySpending> = emptyList(),
    val categoriesById: Map<String, TransactionCategory> = emptyMap(),
    val budgetCategories: Map<String, BudgetCategory> = emptyMap(),
    val transactionCounts: Map<String, Int> = emptyMap(),
    val totalSpending: Double = 0.0,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val showMonthPicker: Boolean = false
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()
    
    init {
        loadCategoryData()
    }
    
    fun loadCategoryData() {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, state.selectedYear)
                    set(Calendar.MONTH, state.selectedMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfMonth = calendar.timeInMillis
                
                // Load categories
                val allCategories = transactionRepository.getAllActiveCategoriesList()
                val categoriesById = allCategories.associateBy { it.id }
                
                // Load category spending for selected month
                var categorySpending = transactionRepository.getCurrentMonthCategorySpending(
                    startOfMonth,
                    endOfMonth
                )
                
                // FALLBACK: If current month has no data, check all-time spending
                if (categorySpending.isEmpty()) {
                    categorySpending = transactionRepository.getAllTimeCategorySpending()
                }
                
                // Load budget categories
                val budget = transactionRepository.getSingleBudget()
                val budgetCategoriesList = if (budget != null) {
                    transactionRepository.getBudgetCategoriesForBudget(budget.id)
                } else {
                    emptyList()
                }
                
                // Match budget categories by category name (same logic as original fragment)
                val budgetCategories = budgetCategoriesList.associate { budgetCat ->
                    // Find category by matching name
                    val categoryId = allCategories.find { cat -> 
                        cat.name.equals(budgetCat.name, ignoreCase = true) 
                    }?.id ?: ""
                    categoryId to budgetCat
                }
                
                // Load transaction counts for each category
                val transactionCounts = categorySpending.associate { spending ->
                    val count = transactionRepository.getTransactionCountByCategory(
                        categoryId = spending.categoryId,
                        startDate = startOfMonth,
                        endDate = endOfMonth
                    )
                    spending.categoryId to count
                }
                
                val totalSpending = categorySpending.sumOf { it.totalAmount }
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categorySpending = categorySpending,
                        categoriesById = categoriesById,
                        budgetCategories = budgetCategories,
                        transactionCounts = transactionCounts,
                        totalSpending = totalSpending
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        categorySpending = emptyList()
                    )
                }
            }
        }
    }
    
    fun setSelectedMonth(month: Int, year: Int) {
        _uiState.update { 
            it.copy(
                selectedMonth = month,
                selectedYear = year,
                showMonthPicker = false
            )
        }
        loadCategoryData()
    }
    
    fun showMonthPicker() {
        _uiState.update { it.copy(showMonthPicker = true) }
    }
    
    fun hideMonthPicker() {
        _uiState.update { it.copy(showMonthPicker = false) }
    }
    
    fun refresh() {
        loadCategoryData()
    }
}

