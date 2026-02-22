package com.koshpal_android.koshpalapp.ui.transactions.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsLoadingState
import com.koshpal_android.koshpalapp.ui.transactions.components.*

/**
 * All Transactions Screen
 * Rebuilt to match HTML design exactly (light & dark themes)
 * 
 * UI-ONLY: No business logic changes
 */
@Composable
fun AllTransactionsScreen(
    transactions: List<Transaction>,
    incomeTotal: Double,
    expenseTotal: Double,
    selectedFilter: String = "All",
    loadingState: TransactionsLoadingState = TransactionsLoadingState.InitialLoading,
    selectedMonth: Pair<Int, Int>? = null,
    onBackClicked: () -> Unit = {},
    onSearchClicked: () -> Unit = {},
    onFilterSelected: (String) -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onMonthSelected: (Int?, Int?) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val stableTransactions = remember(transactions) { transactions }
    val listState = rememberLazyListState()
    
    // Detect when user scrolls to the end for pagination
    LaunchedEffect(listState, loadingState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem?.index?.let { it >= totalItems - 3 } ?: false
        }.collect { isNearEnd ->
            if (isNearEnd && loadingState == TransactionsLoadingState.Success) {
                onLoadMore()
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header: Profile + Greeting + Title + Search
        TransactionsHeader(
            onProfileClick = onProfileClick,
            onSearchClick = onSearchClicked,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Summary Cards: Income & Expense
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "Income",
                amount = incomeTotal,
                isIncome = true,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Expense",
                amount = expenseTotal,
                isIncome = false,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Filter Chips Row
        TransactionsFilterRow(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        // Transactions List
        when (loadingState) {
            TransactionsLoadingState.InitialLoading -> {
                // Show loading state (could add shimmer here if needed)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Loading indicator or shimmer
                }
            }
            TransactionsLoadingState.Success,
            TransactionsLoadingState.LoadingMore -> {
                TransactionList(
                    transactions = stableTransactions,
                    onTransactionClick = onTransactionClick,
                    modifier = Modifier.fillMaxSize(),
                    listState = listState
                )
            }
            TransactionsLoadingState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Error state
                }
            }
        }
    }
}
