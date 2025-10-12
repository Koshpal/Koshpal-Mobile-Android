package com.koshpal_android.koshpalapp.ui.bank

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BankTransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _bankSummary = MutableStateFlow<BankSummary?>(null)
    val bankSummary: StateFlow<BankSummary?> = _bankSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentBankName: String? = null
    private var currentMonthFilter: MonthFilter = MonthFilter.THIS_MONTH
    private var currentTypeFilter: TransactionType? = null

    enum class MonthFilter {
        THIS_MONTH, ALL_TIME
    }

    fun loadBankTransactions(bankName: String, monthFilter: MonthFilter = MonthFilter.THIS_MONTH, typeFilter: TransactionType? = null) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                currentBankName = bankName
                currentMonthFilter = monthFilter
                currentTypeFilter = typeFilter
                
                val allTransactions = transactionRepository.getTransactionsByBank(bankName)
                
                // Apply month filter
                val monthFilteredTransactions = if (monthFilter == MonthFilter.THIS_MONTH) {
                    filterTransactionsForCurrentMonth(allTransactions)
                } else {
                    allTransactions
                }
                
                // Apply type filter
                val filteredTransactions = if (typeFilter != null) {
                    monthFilteredTransactions.filter { it.type == typeFilter }
                } else {
                    monthFilteredTransactions
                }

                _transactions.value = filteredTransactions

                // Calculate summary based on filtered transactions
                val totalSpent = monthFilteredTransactions
                    .filter { it.type == TransactionType.DEBIT }
                    .sumOf { it.amount }
                
                val totalReceived = monthFilteredTransactions
                    .filter { it.type == TransactionType.CREDIT }
                    .sumOf { it.amount }

                _bankSummary.value = BankSummary(
                    bankName = bankName,
                    transactionCount = monthFilteredTransactions.size,
                    totalSpent = totalSpent,
                    totalReceived = totalReceived
                )

            } catch (e: Exception) {
                // Handle error
                _transactions.value = emptyList()
                _bankSummary.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateMonthFilter(monthFilter: MonthFilter) {
        currentBankName?.let { bankName ->
            loadBankTransactions(bankName, monthFilter, currentTypeFilter)
        }
    }
    
    fun updateTypeFilter(typeFilter: TransactionType?) {
        currentBankName?.let { bankName ->
            loadBankTransactions(bankName, currentMonthFilter, typeFilter)
        }
    }
    
    private fun filterTransactionsForCurrentMonth(transactions: List<Transaction>): List<Transaction> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis
        
        return transactions.filter { 
            it.date >= startOfMonth && it.date <= endOfMonth 
        }
    }
}

data class BankSummary(
    val bankName: String,
    val transactionCount: Int,
    val totalSpent: Double,
    val totalReceived: Double
)
