package com.koshpal_android.koshpalapp.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.dao.BudgetCategoryNewDao
import com.koshpal_android.koshpalapp.data.local.dao.BudgetNewDao
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetDao: BudgetNewDao,
    private val categoryDao: BudgetCategoryNewDao
) : ViewModel() {
    
    val uiState: StateFlow<BudgetUiState> = budgetDao.getSingleBudgetFlow()
        .flatMapLatest { budget ->
            if (budget != null) {
                categoryDao.getCategoriesForBudgetFlow(budget.id).combine(flowOf(budget)) { categories, b ->
                    BudgetUiState(budget = b, categories = categories)
                }
            } else {
                flowOf(BudgetUiState(budget = null, categories = emptyList()))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetUiState()
        )
}

data class BudgetUiState(
    val budget: Budget? = null,
    val categories: List<BudgetCategory> = emptyList()
)


