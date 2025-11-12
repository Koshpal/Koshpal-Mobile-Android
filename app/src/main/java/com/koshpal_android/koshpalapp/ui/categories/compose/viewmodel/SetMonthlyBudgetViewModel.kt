package com.koshpal_android.koshpalapp.ui.categories.compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Budget
import com.koshpal_android.koshpalapp.model.BudgetCategory
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import com.koshpal_android.koshpalapp.ui.categories.compose.CategoryBudgetItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class SetMonthlyBudgetUiState(
    val isLoading: Boolean = false,
    val categoryBudgets: List<CategoryBudgetItem> = emptyList(),
    val totalBudget: Double = 0.0,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val showMonthPicker: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SetMonthlyBudgetViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SetMonthlyBudgetUiState())
    val uiState: StateFlow<SetMonthlyBudgetUiState> = _uiState.asStateFlow()
    
    init {
        loadCategoriesWithSpending()
    }
    
    fun loadCategoriesWithSpending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
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
                
                // Get current month spending by category
                val categorySpending = transactionRepository.getCurrentMonthCategorySpending(startOfMonth, endOfMonth)
                val spendingMap = categorySpending.associateBy { it.categoryId }
                
                // Get categories from DB (includes defaults + any custom active categories)
                val allCategories = transactionRepository.getAllActiveCategoriesList()
                    .filter { it.id != "salary" }
                
                // Create category budget items
                val categoryBudgetItems = allCategories.map { category ->
                    val currentSpending = spendingMap[category.id]?.totalAmount ?: 0.0
                    CategoryBudgetItem(
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryIcon = category.icon,
                        categoryColor = category.color,
                        currentSpending = currentSpending,
                        budgetAmount = 0.0
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        categoryBudgets = categoryBudgetItems,
                        totalBudget = categoryBudgetItems.sumOf { it.budgetAmount }
                    )
                }
                
                // Load existing budget after categories are loaded
                loadExistingBudget()
                
            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetViewModel", "Failed to load categories: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load categories: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun loadExistingBudget() {
        viewModelScope.launch {
            try {
                val existingBudget = transactionRepository.getSingleBudget()
                existingBudget?.let { budget ->
                    // Load existing category budgets
                    val categoryBudgets = transactionRepository.getCategoriesForBudget(budget.id)
                    val budgetMap = categoryBudgets.associateBy { it.name }
                    
                    // Update category budget items with existing amounts
                    val updatedCategoryBudgets = _uiState.value.categoryBudgets.map { item ->
                        val existingBudgetCategory = budgetMap[item.categoryName]
                        if (existingBudgetCategory != null) {
                            item.copy(budgetAmount = existingBudgetCategory.allocatedAmount)
                        } else {
                            item
                        }
                    }
                    
                    _uiState.update { 
                        it.copy(
                            categoryBudgets = updatedCategoryBudgets,
                            totalBudget = updatedCategoryBudgets.sumOf { it.budgetAmount }
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetViewModel", "Failed to load existing budget: ${e.message}", e)
            }
        }
    }
    
    fun updateTotalBudget(amount: Double) {
        _uiState.update { it.copy(totalBudget = amount) }
    }
    
    fun updateCategoryBudget(categoryId: String, amount: Double) {
        _uiState.update { state ->
            val updatedBudgets = state.categoryBudgets.map { item ->
                if (item.categoryId == categoryId) {
                    item.copy(budgetAmount = amount)
                } else {
                    item
                }
            }
            state.copy(
                categoryBudgets = updatedBudgets,
                totalBudget = updatedBudgets.sumOf { it.budgetAmount }
            )
        }
    }
    
    fun addCategory(categoryName: String, onSuccess: (CategoryBudgetItem) -> Unit) {
        viewModelScope.launch {
            try {
                // Create custom category with default icon/color
                val created = transactionRepository.insertCustomCategory(categoryName)
                
                // Add to the list with zero budget by default
                val current = _uiState.value.categoryBudgets.toMutableList()
                val exists = current.any { 
                    it.categoryId == created.id || it.categoryName.equals(created.name, true) 
                }
                
                if (!exists) {
                    val newItem = CategoryBudgetItem(
                        categoryId = created.id,
                        categoryName = created.name,
                        categoryIcon = created.icon,
                        categoryColor = created.color,
                        currentSpending = 0.0,
                        budgetAmount = 0.0
                    )
                    current.add(newItem)
                    _uiState.update { 
                        it.copy(categoryBudgets = current)
                    }
                    onSuccess(newItem)
                }
            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetViewModel", "Failed to add category: ${e.message}", e)
                _uiState.update { 
                    it.copy(errorMessage = "Failed to add category: ${e.message}")
                }
            }
        }
    }
    
    fun saveBudget(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val categoryBudgets = _uiState.value.categoryBudgets
                val totalBudget = categoryBudgets.sumOf { it.budgetAmount }
                
                if (totalBudget <= 0) {
                    onError("Please set budget for at least one category")
                    return@launch
                }
                
                // Check if budget already exists
                val existingBudget = transactionRepository.getSingleBudget()
                val budgetId = if (existingBudget != null) {
                    // Update existing budget
                    android.util.Log.d("SetMonthlyBudgetViewModel", "🔄 Updating existing budget: Total ₹$totalBudget")
                    val updatedBudget = existingBudget.copy(
                        totalBudget = totalBudget,
                        savings = 0.0
                    )
                    transactionRepository.updateBudget(updatedBudget)
                    android.util.Log.d("SetMonthlyBudgetViewModel", "✅ Budget updated with ID: ${existingBudget.id}")
                    existingBudget.id
                } else {
                    // Create new budget
                    android.util.Log.d("SetMonthlyBudgetViewModel", "💰 Creating new budget: Total ₹$totalBudget")
                    val budget = Budget(
                        totalBudget = totalBudget,
                        savings = 0.0
                    )
                    val newBudgetId = transactionRepository.insertBudget(budget)
                    android.util.Log.d("SetMonthlyBudgetViewModel", "✅ Budget created with ID: $newBudgetId")
                    newBudgetId.toInt()
                }
                
                // Clear existing budget categories for this budget
                android.util.Log.d("SetMonthlyBudgetViewModel", "🗑️ Clearing existing budget categories for budget ID: $budgetId")
                transactionRepository.clearBudgetCategoriesForBudget(budgetId)
                
                // Create budget categories
                val budgetCategories = categoryBudgets
                    .filter { it.budgetAmount > 0 }
                    .map { categoryBudget ->
                        BudgetCategory(
                            budgetId = budgetId,
                            name = categoryBudget.categoryName,
                            allocatedAmount = categoryBudget.budgetAmount,
                            spentAmount = categoryBudget.currentSpending
                        )
                    }
                
                android.util.Log.d("SetMonthlyBudgetViewModel", "📊 Creating ${budgetCategories.size} budget categories:")
                budgetCategories.forEach { category ->
                    android.util.Log.d("SetMonthlyBudgetViewModel", "   - ${category.name}: ₹${category.allocatedAmount}")
                }
                
                if (budgetCategories.isNotEmpty()) {
                    transactionRepository.insertAllBudgetCategories(budgetCategories)
                    android.util.Log.d("SetMonthlyBudgetViewModel", "✅ Budget categories saved successfully")
                } else {
                    android.util.Log.w("SetMonthlyBudgetViewModel", "⚠️ No budget categories to save")
                }
                
                // Note: Budget notification flags reset should be handled in Fragment/Activity
                // as it requires Android Context which is not available in ViewModel
                
                onSuccess()
                
            } catch (e: Exception) {
                android.util.Log.e("SetMonthlyBudgetViewModel", "Failed to save budget: ${e.message}", e)
                onError("Failed to save budget: ${e.message}")
            }
        }
    }
    
    fun setSelectedMonth(month: Int, year: Int) {
        _uiState.update { it.copy(selectedMonth = month, selectedYear = year) }
        loadCategoriesWithSpending()
    }
    
    fun showMonthPicker() {
        _uiState.update { it.copy(showMonthPicker = true) }
    }
    
    fun hideMonthPicker() {
        _uiState.update { it.copy(showMonthPicker = false) }
    }
}

