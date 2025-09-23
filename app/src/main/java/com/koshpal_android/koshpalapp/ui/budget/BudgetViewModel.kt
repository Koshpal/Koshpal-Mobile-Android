package com.koshpal_android.koshpalapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.dao.BudgetCategoryNewDao
import com.koshpal_android.koshpalapp.data.local.dao.BudgetNewDao
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetDao: BudgetNewDao,
    private val categoryDao: BudgetCategoryNewDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    
    fun load() {
        viewModelScope.launch {
            val budget = budgetDao.getSingleBudget()
            val categories = if (budget != null) categoryDao.getCategoriesForBudget(budget.id) else emptyList()
            _uiState.value = BudgetUiState(budget = budget, categories = categories)
        }
    }
}

data class BudgetUiState(
    val budget: Budget? = null,
    val categories: List<BudgetCategory> = emptyList()
)


