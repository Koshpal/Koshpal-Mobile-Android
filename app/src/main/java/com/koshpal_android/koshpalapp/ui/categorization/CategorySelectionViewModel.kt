package com.koshpal_android.koshpalapp.ui.categorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.data.local.dao.CategoryDao
import com.koshpal_android.koshpalapp.model.TransactionCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategorySelectionViewModel @Inject constructor(
    private val categoryDao: CategoryDao
) : ViewModel() {
    
    private val _categories = MutableStateFlow<List<TransactionCategory>>(emptyList())
    val categories: StateFlow<List<TransactionCategory>> = _categories.asStateFlow()
    
    private val _filteredCategories = MutableStateFlow<List<TransactionCategory>>(emptyList())
    val filteredCategories: StateFlow<List<TransactionCategory>> = _filteredCategories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var allCategories: List<TransactionCategory> = emptyList()
    
    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                categoryDao.getAllActiveCategories().collect { categoryList ->
                    allCategories = categoryList
                    _categories.value = categoryList
                    _filteredCategories.value = categoryList
                }
            } catch (e: Exception) {
                // Handle error
                _categories.value = TransactionCategory.getDefaultCategories()
                _filteredCategories.value = TransactionCategory.getDefaultCategories()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchCategories(query: String) {
        if (query.isBlank()) {
            _filteredCategories.value = allCategories
        } else {
            val filtered = allCategories.filter { category ->
                category.name.contains(query, ignoreCase = true) ||
                category.keywords.any { keyword ->
                    keyword.contains(query, ignoreCase = true)
                }
            }
            _filteredCategories.value = filtered
        }
    }
    
    fun createCustomCategory(category: TransactionCategory) {
        viewModelScope.launch {
            try {
                categoryDao.insertCategory(category)
                loadCategories() // Refresh the list
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
