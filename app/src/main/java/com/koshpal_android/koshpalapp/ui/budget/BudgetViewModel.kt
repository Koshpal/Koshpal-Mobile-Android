package com.koshpal_android.koshpalapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.repository.BudgetRepository
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class BudgetUiState(
    val isLoading: Boolean = false,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val errorMessage: String? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    val budgets: StateFlow<List<Budget>> = _budgets.asStateFlow()
    
    fun createBudget(amount: Double, isMonthly: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val calendar = Calendar.getInstance()
                budgetRepository.createBudget(
                    categoryId = "default", // Default category
                    limit = amount,
                    month = calendar.get(Calendar.MONTH) + 1,
                    year = calendar.get(Calendar.YEAR)
                )
                loadBudgets()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalBudget = amount,
                    remainingBudget = amount
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create budget: ${e.message}"
                )
            }
        }
    }
    
    fun loadBudgets() {
        viewModelScope.launch {
            try {
                budgetRepository.getAllActiveBudgets().collect { budgetList ->
                    _budgets.value = budgetList
                    updateBudgetOverview(budgetList)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load budgets: ${e.message}"
                )
            }
        }
    }
    
    private fun updateBudgetOverview(budgets: List<Budget>) {
        val totalBudget = budgets.sumOf { it.monthlyLimit }
        val totalSpent = budgets.sumOf { it.spent }
        val remaining = totalBudget - totalSpent
        
        _uiState.value = _uiState.value.copy(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remainingBudget = remaining
        )
    }
}
