package com.koshpal_android.koshpalapp.ui.transactions.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import com.koshpal_android.koshpalapp.ui.transactions.TransactionsLoadingState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Internal enum for transaction type info (for memoization)
 */
private enum class TransactionTypeInfo {
    Income, Expense, Transfer
}

/**
 * Data class to hold all computed transaction item info
 * This reduces the number of remember blocks and improves performance
 */
@Immutable
private data class TransactionItemInfo(
    val formattedDate: String,
    val iconColor: Color,
    val amountColor: Color,
    val amountPrefix: String,
    val typeLabel: String,
    val categoryIcon: Int, // Drawable resource ID for category icon
    val arrowIcon: Int, // Drawable resource ID for arrow direction icon (diagonal)
    val displayText: String,
    val formattedAmount: String
)

/**
 * Stateless All Transactions Screen Composable
 * Optimized for performance with pagination and shimmer loading
 * 
 * @param transactions List of transactions to display (paginated)
 * @param incomeTotal Total income amount
 * @param expenseTotal Total expense amount
 * @param selectedFilter Currently selected filter (e.g., "All", "Income", "Expense")
 * @param loadingState Current loading state (InitialLoading, Success, LoadingMore, Error)
 * @param onBackClicked Callback when back button is clicked
 * @param onSearchClicked Callback when search icon is clicked
 * @param onFilterSelected Callback when a filter chip is selected (filter name as parameter)
 * @param onTransactionClick Callback when a transaction item is clicked
 * @param onLoadMore Callback when user scrolls to end and more data should be loaded
 * @param selectedMonth Currently selected month (Pair<Month, Year> or null for "All")
 * @param onMonthSelected Callback when month is selected from dropdown
 */
@Composable
fun AllTransactionsScreen(
    transactions: List<Transaction>,
    incomeTotal: Double,
    expenseTotal: Double,
    selectedFilter: String = "All",
    loadingState: TransactionsLoadingState = TransactionsLoadingState.InitialLoading,
    selectedMonth: Pair<Int, Int>? = null,
    onBackClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onFilterSelected: (String) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onLoadMore: () -> Unit = {},
    onMonthSelected: (Int?, Int?) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Memoize the transactions list to prevent unnecessary recompositions
    val stableTransactions = remember(transactions) { transactions }
    val listState = rememberLazyListState()
    
    // Detect when user scrolls to the end
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
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.backgroundstrucure2),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Common App Bar
        com.koshpal_android.koshpalapp.ui.common.CommonAppBar(
            onProfileClick = onProfileClick,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Screen Title and Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                color = AppColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onSearchClicked) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = AppColors.TextPrimary
                )
            }
        }
        
        // Summary Cards (Income & Expense) - only recompose when totals change
        SummaryCards(
            incomeTotal = incomeTotal,
            expenseTotal = expenseTotal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        
        // Filter Chips - only recompose when filter changes
        FilterChips(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Transactions List - optimized LazyColumn with pagination
        TransactionsList(
            transactions = stableTransactions,
            loadingState = loadingState,
            listState = listState,
            onTransactionClick = onTransactionClick,
            onLoadMore = onLoadMore,
            modifier = Modifier.fillMaxSize()
        )
        }
    }
}

/**
 * Top App Bar with back button, month dropdown, title, and search icon
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    selectedMonth: Pair<Int, Int>?,
    onBackClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onMonthSelected: (Int?, Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    
    // Generate list of months (last 12 months + "All")
    val monthOptions = remember {
        val months = mutableListOf<Pair<String, Pair<Int, Int>?>>()
        for (i in 11 downTo 0) {
            val monthCalendar = Calendar.getInstance()
            monthCalendar.add(Calendar.MONTH, -i)
            val month = monthCalendar.get(Calendar.MONTH)
            val year = monthCalendar.get(Calendar.YEAR)
            val monthName = monthCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            months.add("$monthName $year" to Pair(month, year))
        }
        months.add("All" to null)
        months
    }
    
    val selectedMonthText = remember(selectedMonth) {
        if (selectedMonth == null) {
            "All"
        } else {
            val (month, year) = selectedMonth
            val monthCalendar = Calendar.getInstance()
            monthCalendar.set(year, month, 1)
            monthCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) + " $year"
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(onClick = onBackClicked) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = AppColors.TextPrimary
            )
        }
        
        // Month Dropdown (before "All Transactions" text)
        Box {
            Row(
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedMonthText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Select month",
                    tint = AppColors.TextPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(90f)
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(AppColors.DarkCard)
                    .width(180.dp)
            ) {
                monthOptions.forEach { (label, monthYear) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                color = if (selectedMonth == monthYear) AppColors.AccentBlue else AppColors.TextPrimary
                            )
                        },
                        onClick = {
                            onMonthSelected(monthYear?.first, monthYear?.second)
                            expanded = false
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = AppColors.TextPrimary
                        )
                    )
                }
            }
        }
        
        // Title
        Text(
            text = "All Transactions",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        
        // Search Icon
        IconButton(onClick = onSearchClicked) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = AppColors.TextPrimary
            )
        }
    }
}

/**
 * Income and Expense Summary Cards
 */
@Composable
private fun SummaryCards(
    incomeTotal: Double,
    expenseTotal: Double,
    modifier: Modifier = Modifier
) {
    // Memoize formatted amounts to avoid recomputation
    val formattedIncome = remember(incomeTotal) {
        String.format("%.2f", incomeTotal)
    }
    val formattedExpense = remember(expenseTotal) {
        String.format("%.2f", expenseTotal)
    }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Card
        SummaryCard(
            label = "Income",
            amount = incomeTotal,
            formattedAmount = formattedIncome,
            isIncome = true,
            modifier = Modifier.weight(1f)
        )
        
        // Expense Card
        SummaryCard(
            label = "Expense",
            amount = expenseTotal,
            formattedAmount = formattedExpense,
            isIncome = false,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Individual Summary Card (Income or Expense)
 */
@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    formattedAmount: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    // Memoize colors to avoid recreation
    val iconColor = remember(isIncome) {
        if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    }
    val amountColor = remember(isIncome) {
        if (isIncome) Color(0xFF4CAF50) else AppColors.TextPrimary
    }
    val glowColor = remember(isIncome) {
        if (isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    }
    
    // Use Card without shadow - shadows are very expensive and cause lag
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // No elevation = no shadow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon and Label Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    painter = painterResource(id = if (isIncome) {
                        com.koshpal_android.koshpalapp.R.drawable.arrowdown
                    } else {
                        com.koshpal_android.koshpalapp.R.drawable.arrowup
                    }),
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Amount
            Text(
                text = "₹$formattedAmount",
                style = MaterialTheme.typography.titleLarge,
                color = amountColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Filter Chips Row (Horizontally Scrollable)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf("All", "Income", "Expense", "This Month", "Last Month", "Starred", "Cashflow")
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (filter == selectedFilter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColors.AccentBlue,
                    selectedLabelColor = AppColors.TextPrimary,
                    containerColor = AppColors.DarkCard,
                    labelColor = AppColors.TextSecondary
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

/**
 * Shimmer placeholder for transaction item
 * Uses gradient sweep animation like Facebook Shimmer library
 */
@Composable
private fun TransactionShimmerItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    // Animate the shimmer gradient offset from left to right
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    
    // Base color for shimmer (dark gray) - matches dark theme
    val baseColor = Color(0xFF1C1C1E)
    // Highlight color (lighter gray for shimmer effect) - more visible
    val highlightColor = Color(0xFF3A3A3C)
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon and Main Content
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Circle Shimmer with gradient
                ShimmerBox(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    shimmerOffset = shimmerOffset,
                    baseColor = baseColor,
                    highlightColor = highlightColor
                )
                
                // Main Content Column Shimmer
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title shimmer
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp),
                        shape = RoundedCornerShape(4.dp),
                        shimmerOffset = shimmerOffset,
                        baseColor = baseColor,
                        highlightColor = highlightColor
                    )
                    
                    // Date shimmer
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(12.dp),
                        shape = RoundedCornerShape(4.dp),
                        shimmerOffset = shimmerOffset,
                        baseColor = baseColor,
                        highlightColor = highlightColor
                    )
                }
            }
            
            // Right: Amount Shimmer
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Amount shimmer
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(16.dp),
                    shape = RoundedCornerShape(4.dp),
                    shimmerOffset = shimmerOffset,
                    baseColor = baseColor,
                    highlightColor = highlightColor
                )
                
                // Type label shimmer
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(12.dp),
                    shape = RoundedCornerShape(4.dp),
                    shimmerOffset = shimmerOffset,
                    baseColor = baseColor,
                    highlightColor = highlightColor
                )
            }
        }
    }
}

/**
 * Shimmer box with gradient sweep effect
 */
@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    shimmerOffset: Float,
    baseColor: Color,
    highlightColor: Color
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor,
                        baseColor,
                        highlightColor.copy(alpha = 0.8f),
                        highlightColor,
                        highlightColor.copy(alpha = 0.8f),
                        baseColor,
                        baseColor
                    ),
                    start = Offset(
                        x = shimmerOffset * 300f - 150f,
                        y = 0f
                    ),
                    end = Offset(
                        x = shimmerOffset * 300f + 150f,
                        y = 0f
                    )
                )
            )
    )
}

/**
 * Transactions List with pagination and shimmer support
 */
@Composable
private fun TransactionsList(
    transactions: List<Transaction>,
    loadingState: TransactionsLoadingState,
    listState: LazyListState,
    onTransactionClick: (Transaction) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (loadingState) {
            TransactionsLoadingState.InitialLoading -> {
                // Show shimmer placeholders
                items(count = 12, key = { "shimmer_$it" }) {
                    TransactionShimmerItem()
                }
            }
            TransactionsLoadingState.Success, TransactionsLoadingState.LoadingMore -> {
                if (transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No transactions found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                } else {
                    // Show actual transactions
                    items(
                        count = transactions.size,
                        key = { index -> transactions[index].id },
                        contentType = { "transaction_item" }
                    ) { index ->
                        TransactionItem(
                            transaction = transactions[index],
                            onClick = { onTransactionClick(transactions[index]) }
                        )
                    }
                    
                    // Show loading spinner at bottom when loading more
                    if (loadingState == TransactionsLoadingState.LoadingMore) {
                        item(key = "loading_more") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = AppColors.AccentBlue
                                )
                            }
                        }
                    }
                }
            }
            TransactionsLoadingState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error loading transactions",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual Transaction Item
 * Heavily optimized for performance - minimal recompositions
 * Uses category icons and proper arrow directions
 */
@Composable
private fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    // Get category info
    val category = remember(transaction.categoryId) {
        TransactionCategory.getDefaultCategories().find { it.id == transaction.categoryId }
            ?: TransactionCategory.getDefaultCategories().find { it.id == "others" }
    }
    
    // Use derivedStateOf for all computed values - only recomputes when dependencies change
    val transactionInfo = remember(
        transaction.type,
        transaction.timestamp,
        transaction.amount,
        transaction.merchant,
        transaction.description,
        category
    ) {
        val dateFormatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val formattedDate = dateFormatter.format(Date(transaction.timestamp))
        
        val type = when (transaction.type) {
            TransactionType.CREDIT -> TransactionTypeInfo.Income
            TransactionType.DEBIT -> TransactionTypeInfo.Expense
            TransactionType.TRANSFER -> TransactionTypeInfo.Transfer
        }
        
        // Get category color or default based on type
        val categoryColor = try {
            Color(android.graphics.Color.parseColor(category?.color ?: "#607D8B"))
        } catch (e: Exception) {
            when (type) {
                TransactionTypeInfo.Income -> Color(0xFF4CAF50)
                TransactionTypeInfo.Expense -> Color(0xFFF44336)
                TransactionTypeInfo.Transfer -> AppColors.AccentBlue
            }
        }
        
        TransactionItemInfo(
            formattedDate = formattedDate,
            iconColor = categoryColor,
            amountColor = when (type) {
                TransactionTypeInfo.Income -> Color(0xFF4CAF50)
                TransactionTypeInfo.Expense -> AppColors.TextPrimary
                TransactionTypeInfo.Transfer -> AppColors.TextPrimary
            },
            amountPrefix = when (type) {
                TransactionTypeInfo.Income -> "+"
                TransactionTypeInfo.Expense -> "-"
                TransactionTypeInfo.Transfer -> ""
            },
            typeLabel = when (type) {
                TransactionTypeInfo.Income -> "Income"
                TransactionTypeInfo.Expense -> "Expense"
                TransactionTypeInfo.Transfer -> "Transfer"
            },
            categoryIcon = category?.icon ?: com.koshpal_android.koshpalapp.R.drawable.ic_category_default,
            arrowIcon = when (type) {
                TransactionTypeInfo.Income -> com.koshpal_android.koshpalapp.R.drawable.arrowdown
                TransactionTypeInfo.Expense -> com.koshpal_android.koshpalapp.R.drawable.arrowup
                TransactionTypeInfo.Transfer -> com.koshpal_android.koshpalapp.R.drawable.ic_arrow_swap_horizontal
            },
            displayText = transaction.merchant.ifEmpty { transaction.description },
            formattedAmount = when (type) {
                TransactionTypeInfo.Income -> "+₹${String.format("%.2f", transaction.amount)}"
                TransactionTypeInfo.Expense -> "-₹${String.format("%.2f", transaction.amount)}"
                TransactionTypeInfo.Transfer -> "₹${String.format("%.2f", transaction.amount)}"
            }
        )
    }
    
    // Use Card without shadow - much faster, no shadow overhead
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // No elevation = no shadow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon and Main Content
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Circle - uses actual category icon and color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(transactionInfo.iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = transactionInfo.categoryIcon),
                        contentDescription = transactionInfo.typeLabel,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Main Content Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = transactionInfo.displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = transactionInfo.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
            
            // Right: Amount and Arrow
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = transactionInfo.formattedAmount,
                        style = MaterialTheme.typography.bodyLarge,
                        color = transactionInfo.amountColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = transactionInfo.typeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary
                    )
                }
                
                // Arrow Icon - diagonal direction based on transaction type (up-right for income, down-right for expense)
                Image(
                    painter = painterResource(id = transactionInfo.arrowIcon),
                    contentDescription = "View details",
                    colorFilter = ColorFilter.tint(AppColors.TextSecondary),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

