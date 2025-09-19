package com.koshpal_android.koshpalapp.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _filteredTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _filteredTransactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _summaryData = MutableStateFlow(TransactionSummary())
    val summaryData: StateFlow<TransactionSummary> = _summaryData.asStateFlow()

    private val _currentFilter = MutableStateFlow("all")
    private val _searchQuery = MutableStateFlow("")

    init {
        // Combine filter and search to update filtered transactions
        viewModelScope.launch {
            combine(
                _allTransactions,
                _currentFilter,
                _searchQuery
            ) { transactions, filter, query ->
                applyFiltersAndSearch(transactions, filter, query)
            }.collect { filtered ->
                _filteredTransactions.value = filtered
                updateSummary(filtered)
            }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                transactionRepository.getAllTransactions().collect { transactions ->
                    _allTransactions.value = transactions
                }
            } catch (e: Exception) {
                // Handle error
                _allTransactions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterTransactions(filter: String) {
        _currentFilter.value = filter
    }

    fun searchTransactions(query: String) {
        _searchQuery.value = query
    }

    fun onTransactionClick(transaction: Transaction) {
        // Handle transaction click - navigate to details or categorization
        // This could emit a navigation event
    }

    fun showFilterOptions() {
        // Show filter bottom sheet or dialog
        // This could emit a UI event
    }

    private fun applyFiltersAndSearch(
        transactions: List<Transaction>,
        filter: String,
        query: String
    ): List<Transaction> {
        var filtered = transactions

        // Apply type filter
        filtered = when (filter) {
            "income" -> filtered.filter { it.type == TransactionType.CREDIT }
            "expense" -> filtered.filter { it.type == TransactionType.DEBIT }
            "this_month" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                
                filtered.filter { transaction ->
                    calendar.timeInMillis = transaction.timestamp
                    calendar.get(Calendar.MONTH) == currentMonth && 
                    calendar.get(Calendar.YEAR) == currentYear
                }
            }
            "last_month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastMonthYear = calendar.get(Calendar.YEAR)
                
                filtered.filter { transaction ->
                    calendar.timeInMillis = transaction.timestamp
                    calendar.get(Calendar.MONTH) == lastMonth && 
                    calendar.get(Calendar.YEAR) == lastMonthYear
                }
            }
            else -> filtered // "all"
        }

        // Apply search query
        if (query.isNotBlank()) {
            filtered = filtered.filter { transaction ->
                transaction.merchant.contains(query, ignoreCase = true) ||
                transaction.description.contains(query, ignoreCase = true) ||
                transaction.amount.toString().contains(query)
            }
        }

        return filtered.sortedByDescending { it.timestamp }
    }

    private fun updateSummary(transactions: List<Transaction>) {
        val totalIncome = transactions
            .filter { it.type == TransactionType.CREDIT }
            .sumOf { it.amount }
        
        val totalExpense = transactions
            .filter { it.type == TransactionType.DEBIT }
            .sumOf { it.amount }

        _summaryData.value = TransactionSummary(
            totalIncome = totalIncome,
            totalExpense = totalExpense
        )
    }
}

data class TransactionSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)
