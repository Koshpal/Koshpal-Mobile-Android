package com.koshpal_android.koshpalapp.ui.budget

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.dao.BudgetCategoryNewDao
import com.koshpal_android.koshpalapp.data.local.dao.BudgetNewDao
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetEditViewModel @Inject constructor(
    private val budgetDao: BudgetNewDao,
    private val categoryDao: BudgetCategoryNewDao
) : ViewModel() {

    private val _uiState = MutableLiveData(EditUiState())
    val uiState: LiveData<EditUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            val existing = budgetDao.getSingleBudget()
            if (existing == null) {
                _uiState.value = EditUiState(
                    totalBudgetText = "",
                    categories = defaultCategories()
                )
            } else {
                val cats = categoryDao.getCategoriesForBudget(existing.id)
                _uiState.value = EditUiState(
                    totalBudgetText = existing.totalBudget.toInt().toString(),
                    categories = cats
                )
            }
        }
    }

    private fun defaultCategories(): List<BudgetCategory> {
        val names = listOf("Rent", "Food", "Entertainment", "EMI", "Travel", "Others")
        return names.map { n -> BudgetCategory(budgetId = 0, name = n, allocatedAmount = 0.0) }
    }

    fun updateCategoryValue(name: String, value: Double) {
        val current = _uiState.value ?: return
        val updated = current.categories.map { if (it.name == name) it.copy(allocatedAmount = value) else it }
        _uiState.value = current.copy(categories = updated, error = null)
    }

    fun autoDistribute() {
        val current = _uiState.value ?: return
        val total = current.totalBudgetText.toDoubleOrNull() ?: 0.0
        if (total <= 0.0 || current.categories.isEmpty()) return
        // Realistic default allocation percentages (sum = 100%)
        val weights = mapOf(
            "Rent" to 0.30,
            "Food" to 0.20,
            "Entertainment" to 0.10,
            "EMI" to 0.15,
            "Travel" to 0.10,
            "Others" to 0.15
        )
        val distributed = current.categories.map { c ->
            val w = weights[c.name] ?: (1.0 / current.categories.size)
            c.copy(allocatedAmount = (total * w))
        }
        _uiState.value = current.copy(categories = distributed, error = null)
    }

    fun save(totalBudgetText: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val total = totalBudgetText.toDoubleOrNull() ?: 0.0
            val cats = _uiState.value?.categories ?: emptyList()
            val allocated = cats.sumOf { it.allocatedAmount }
            if (allocated > total) {
                _uiState.value = _uiState.value?.copy(error = "Please enter distribution that does not exceed your total budget.")
                return@launch
            }
            val savings = (total - allocated).coerceAtLeast(0.0)

            val existing = budgetDao.getSingleBudget()
            val budgetId = if (existing == null) {
                budgetDao.clearBudgets()
                budgetDao.insertBudget(Budget(totalBudget = total, savings = savings)).toInt()
            } else {
                val updatedBudget = existing.copy(totalBudget = total, savings = savings)
                budgetDao.updateBudget(updatedBudget)
                existing.id
            }

            categoryDao.clearForBudget(budgetId)
            val toInsert = cats.map { it.copy(id = 0, budgetId = budgetId) }
            categoryDao.insertAll(toInsert)
            onDone()
        }
    }
}

data class EditUiState(
    val totalBudgetText: String = "",
    val categories: List<BudgetCategory> = emptyList(),
    val error: String? = null
)


