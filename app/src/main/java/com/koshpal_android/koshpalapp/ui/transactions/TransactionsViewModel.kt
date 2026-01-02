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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * Loading state for transactions
 */
enum class TransactionsLoadingState {
    InitialLoading,    // First load - show shimmer
    Success,          // Data loaded successfully
    LoadingMore,      // Loading next page - show spinner at bottom
    Error            // Error state
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _displayedTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val displayedTransactions: StateFlow<List<Transaction>> = _displayedTransactions.asStateFlow()

    private val _loadingState = MutableStateFlow<TransactionsLoadingState>(TransactionsLoadingState.InitialLoading)
    val loadingState: StateFlow<TransactionsLoadingState> = _loadingState.asStateFlow()

    private val _summaryData = MutableStateFlow(TransactionSummary())
    val summaryData: StateFlow<TransactionSummary> = _summaryData.asStateFlow()

    private val _currentFilter = MutableStateFlow("All")
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _selectedMonth = MutableStateFlow<Pair<Int, Int>?>(null) // Month, Year (null = All)
    val selectedMonth: StateFlow<Pair<Int, Int>?> = _selectedMonth.asStateFlow()
    
    private val _cashFlowTransactionIds = MutableStateFlow<Set<String>>(emptySet())
    
    private var currentPage = 0
    private var hasMoreData = true

    init {
        // Combine filter and search to update filtered transactions
        // Use distinctUntilChanged to avoid unnecessary recomputations
        viewModelScope.launch {
            combine(
                _allTransactions,
                _currentFilter,
                _searchQuery,
                _selectedMonth
            ) { transactions, filter, query, monthFilter ->
                val filtered = applyFiltersAndSearch(transactions, filter, query, monthFilter)
                // Only update displayed transactions if we're not in initial loading
                // and if filter/search actually changed (to reset pagination)
                if (_loadingState.value != TransactionsLoadingState.InitialLoading) {
                    // Reset pagination when filter/search changes
                    currentPage = 0
                    hasMoreData = filtered.isNotEmpty()
                    updateDisplayedTransactions(filtered)
                }
                filtered
            }
            .distinctUntilChanged()
            .collect { filtered ->
                updateSummary(filtered)
            }
        }
    }

    /**
     * Load initial page of transactions
     * Only loads first page to fill screen, not all transactions
     */
    fun loadTransactions() {
        viewModelScope.launch {
            _loadingState.value = TransactionsLoadingState.InitialLoading
            currentPage = 0
            hasMoreData = true
            _displayedTransactions.value = emptyList() // Clear previous data
            
            try {
                // Load cashflow transaction IDs from database
                val cashFlowTransactions = transactionRepository.getCashFlowTransactions()
                _cashFlowTransactionIds.value = cashFlowTransactions.map { it.id }.toSet()
                
                // Get first emission from Flow (all transactions from DB)
                val allTransactions = transactionRepository.getAllTransactions().first()
                _allTransactions.value = allTransactions
                
                // Apply current filter and get first page only
                val filtered = applyFiltersAndSearch(
                    allTransactions,
                    _currentFilter.value,
                    _searchQuery.value,
                    _selectedMonth.value
                )
                
                // Only show first page initially
                updateDisplayedTransactions(filtered)
                _loadingState.value = TransactionsLoadingState.Success
                
                // Continue observing for changes (but don't reset pagination)
                transactionRepository.getAllTransactions()
                    .collect { transactions ->
                        if (transactions != _allTransactions.value) {
                            _allTransactions.value = transactions
                            // Re-apply filter and update displayed items
                            val newFiltered = applyFiltersAndSearch(
                                transactions,
                                _currentFilter.value,
                                _searchQuery.value,
                                _selectedMonth.value
                            )
                            // Keep current page, just update the data
                            val endIndex = minOf((currentPage + 1) * PAGE_SIZE, newFiltered.size)
                            _displayedTransactions.value = newFiltered.take(endIndex)
                            hasMoreData = endIndex < newFiltered.size
                        }
                    }
            } catch (e: Exception) {
                _allTransactions.value = emptyList()
                _displayedTransactions.value = emptyList()
                _loadingState.value = TransactionsLoadingState.Error
            }
        }
    }

    /**
     * Load next page of transactions
     */
    fun loadMoreTransactions() {
        if (!hasMoreData || _loadingState.value == TransactionsLoadingState.LoadingMore) {
            return
        }

        viewModelScope.launch {
            _loadingState.value = TransactionsLoadingState.LoadingMore
            
            try {
                val allFiltered = applyFiltersAndSearch(
                    _allTransactions.value,
                    _currentFilter.value,
                    _searchQuery.value,
                    _selectedMonth.value
                )
                
                val nextPage = currentPage + 1
                val startIndex = nextPage * PAGE_SIZE
                val endIndex = minOf(startIndex + PAGE_SIZE, allFiltered.size)
                
                if (startIndex < allFiltered.size) {
                    val newItems = allFiltered.subList(startIndex, endIndex)
                    _displayedTransactions.value = _displayedTransactions.value + newItems
                    currentPage = nextPage
                    hasMoreData = endIndex < allFiltered.size
                } else {
                    hasMoreData = false
                }
                
                _loadingState.value = TransactionsLoadingState.Success
            } catch (e: Exception) {
                _loadingState.value = TransactionsLoadingState.Error
            }
        }
    }

    /**
     * Update displayed transactions based on current page
     */
    private fun updateDisplayedTransactions(filtered: List<Transaction>) {
        val endIndex = minOf((currentPage + 1) * PAGE_SIZE, filtered.size)
        _displayedTransactions.value = filtered.take(endIndex)
        hasMoreData = endIndex < filtered.size
    }

    fun filterTransactions(filter: String) {
        if (_currentFilter.value != filter) {
            _currentFilter.value = filter
            // Reset to first page when filter changes
            currentPage = 0
        }
    }

    fun searchTransactions(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedMonth(month: Int?, year: Int?) {
        _selectedMonth.value = if (month != null && year != null) {
            Pair(month, year)
        } else {
            null // "All" option
        }
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
        query: String,
        monthFilter: Pair<Int, Int>?
    ): List<Transaction> {
        var filtered = transactions

        // Apply month filter first
        if (monthFilter != null) {
            val (month, year) = monthFilter
            val calendar = Calendar.getInstance()
            calendar.set(year, month, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis
            
            filtered = filtered.filter { transaction ->
                transaction.timestamp in startOfMonth..endOfMonth
            }
        }

        // Apply type filter
        filtered = when (filter.lowercase()) {
            "income" -> filtered.filter { it.type == TransactionType.CREDIT }
            "expense" -> filtered.filter { it.type == TransactionType.DEBIT }
            "this month" -> {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                
                // Calculate month boundaries once
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfMonth = calendar.timeInMillis
                
                // Use timestamp comparison instead of creating Calendar for each transaction
                filtered.filter { transaction ->
                    transaction.timestamp in startOfMonth..endOfMonth
                }
            }
            "last month" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                val lastMonth = calendar.get(Calendar.MONTH)
                val lastMonthYear = calendar.get(Calendar.YEAR)
                
                // Calculate month boundaries once
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfLastMonth = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfLastMonth = calendar.timeInMillis
                
                // Use timestamp comparison instead of creating Calendar for each transaction
                filtered.filter { transaction ->
                    transaction.timestamp in startOfLastMonth..endOfLastMonth
                }
            }
            "starred" -> filtered.filter { it.isStarred }
            "cashflow" -> filtered.filter { it.id in _cashFlowTransactionIds.value }
            else -> filtered // "All"
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
        // SMART FALLBACK TOTALS: Show most relevant data available
        // 1. Current month (if has transactions)
        // 2. Last 30 days (if has transactions)
        // 3. All-time (fallback)

        val cashFlowTransactionIds = _cashFlowTransactionIds.value

        // Calculate current month totals
        var currentMonthIncome = 0.0
        var currentMonthExpense = 0.0

        // Get current month boundaries
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis

        // Calculate last 30 days totals
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        var last30DaysIncome = 0.0
        var last30DaysExpense = 0.0

        // Calculate all-time totals (excluding cash flow)
        var allTimeIncome = 0.0
        var allTimeExpense = 0.0

        transactions.forEach { transaction ->
            // Skip if transaction is in cash flow
            if (cashFlowTransactionIds.contains(transaction.id)) {
                return@forEach
            }

            // All-time totals
            when (transaction.type) {
                TransactionType.CREDIT -> allTimeIncome += transaction.amount
                TransactionType.DEBIT, TransactionType.TRANSFER -> allTimeExpense += transaction.amount
            }

            // Current month totals
            if (transaction.timestamp in startOfMonth..endOfMonth) {
                when (transaction.type) {
                    TransactionType.CREDIT -> currentMonthIncome += transaction.amount
                    TransactionType.DEBIT, TransactionType.TRANSFER -> currentMonthExpense += transaction.amount
                }
            }

            // Last 30 days totals
            if (transaction.timestamp >= thirtyDaysAgo) {
                when (transaction.type) {
                    TransactionType.CREDIT -> last30DaysIncome += transaction.amount
                    TransactionType.DEBIT, TransactionType.TRANSFER -> last30DaysExpense += transaction.amount
                }
            }
        }

        // Determine which data to display with smart fallback
        val (displayIncome, displayExpense) = when {
            // Option 1: Current month has transactions
            currentMonthIncome > 0 || currentMonthExpense > 0 -> {
                android.util.Log.d("TransactionsViewModel", "📊 SUMMARY: Using current month totals")
                Pair(currentMonthIncome, currentMonthExpense)
            }
            // Option 2: Last 30 days has transactions
            last30DaysIncome > 0 || last30DaysExpense > 0 -> {
                android.util.Log.d("TransactionsViewModel", "📊 SUMMARY: Using last 30 days totals (current month empty)")
                Pair(last30DaysIncome, last30DaysExpense)
            }
            // Option 3: All-time totals (fallback)
            else -> {
                android.util.Log.d("TransactionsViewModel", "📊 SUMMARY: Using all-time totals (no recent activity)")
                Pair(allTimeIncome, allTimeExpense)
            }
        }

        android.util.Log.d("TransactionsViewModel", "📊 SUMMARY TOTALS: Income ₹$displayIncome, Expense ₹$displayExpense")
        android.util.Log.d("TransactionsViewModel", "   Current Month: ₹$currentMonthIncome / ₹$currentMonthExpense")
        android.util.Log.d("TransactionsViewModel", "   Last 30 Days: ₹$last30DaysIncome / ₹$last30DaysExpense")
        android.util.Log.d("TransactionsViewModel", "   All Time: ₹$allTimeIncome / ₹$allTimeExpense")

        _summaryData.value = TransactionSummary(
            totalIncome = displayIncome,
            totalExpense = displayExpense
        )
    }
    
    val currentFilter: StateFlow<String> = _currentFilter.asStateFlow()
}

data class TransactionSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0
)
