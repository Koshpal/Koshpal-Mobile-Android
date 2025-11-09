package com.koshpal_android.koshpalapp.ui.categories.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Add
import androidx.hilt.navigation.compose.hiltViewModel
import com.koshpal_android.koshpalapp.model.CategorySpending
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.ui.categories.compose.viewmodel.CategoriesViewModel
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Categories Screen in Jetpack Compose
 * Displays budget overview, donut chart, and category spending list
 */
@Composable
fun CategoriesScreen(
    onSetBudgetClick: () -> Unit = {},
    onCategoryClick: (String, String, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth = uiState.selectedMonth
    val selectedYear = uiState.selectedYear
    
    // Format month display
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, selectedYear)
        set(Calendar.MONTH, selectedMonth)
    }
    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val monthDisplay = monthFormat.format(calendar.time)
    
    // Month Picker Dialog
    if (uiState.showMonthPicker) {
        MonthPickerDialog(
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            onMonthSelected = { month, year ->
                viewModel.setSelectedMonth(month, year)
            },
            onDismiss = { viewModel.hideMonthPicker() }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.PureBlack)
            .padding(16.dp)
    ) {
        // Top Header
        TopHeader(
            monthDisplay = monthDisplay,
            onMonthClick = { viewModel.showMonthPicker() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Content based on state
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.AccentBlue)
                }
            }
            uiState.categorySpending.isEmpty() -> {
                EmptyState()
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Total Spends Donut Chart Card
                    item {
                        TotalSpendsCard(
                            totalSpending = uiState.totalSpending,
                            categorySpending = uiState.categorySpending,
                            categoriesById = uiState.categoriesById
                        )
                    }
                    
                    // Set Monthly Budget Button
                    item {
                        SetBudgetButton(
                            onClick = onSetBudgetClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Category List Card
                    item {
                        CategoryListCard(
                            categorySpending = uiState.categorySpending,
                            categoriesById = uiState.categoriesById,
                            budgetCategories = uiState.budgetCategories,
                            transactionCounts = uiState.transactionCounts,
                            onCategoryClick = onCategoryClick,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeader(
    monthDisplay: String,
    onMonthClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Budget Title
        Text(
            text = "Budget",
            style = MaterialTheme.typography.headlineLarge,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        // Month Selector
        Row(
            modifier = Modifier
                .clickable(onClick = onMonthClick)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthDisplay,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Month",
                tint = AppColors.TextPrimary
            )
        }
    }
}

@Composable
private fun TotalSpendsCard(
    totalSpending: Double,
    categorySpending: List<CategorySpending>,
    categoriesById: Map<String, TransactionCategory>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .drawBehind {
                // Soft radial gradient glow in bottom-left corner
                val cornerRadius = 16.dp.toPx()
                val glowCenterX = -cornerRadius * 0.5f // Just outside the corner
                val glowCenterY = size.height + cornerRadius * 0.5f
                val glowRadius = 60.dp.toPx()
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentBlue.copy(alpha = 0.4f), // Bright blue at center
                            AppColors.AccentBlue.copy(alpha = 0.1f), // Fade to transparent
                            Color.Transparent
                        ),
                        center = Offset(glowCenterX, glowCenterY),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(glowCenterX, glowCenterY)
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "TOTAL SPENDS" Label
            Text(
                text = "TOTAL SPENDS",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary, // Light gray for descriptive text
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Donut Chart
            DonutChart(
                categorySpending = categorySpending,
                categoriesById = categoriesById,
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total Amount
            Text(
                text = "₹${String.format("%,.0f", totalSpending)}",
                style = MaterialTheme.typography.displayMedium,
                color = AppColors.TextPrimary, // White for main amounts
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SetBudgetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.DarkButtonBg)
            .clickable(onClick = onClick)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AppColors.AccentBlue.copy(alpha = 0.3f),
                ambientColor = AppColors.AccentBlue.copy(alpha = 0.15f)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Set Monthly Budget",
                tint = AppColors.AccentBlue,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Set Monthly Budget",
                color = AppColors.AccentBlue,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CategoryListCard(
    categorySpending: List<CategorySpending>,
    categoriesById: Map<String, TransactionCategory>,
    budgetCategories: Map<String, com.koshpal_android.koshpalapp.model.BudgetCategory>,
    transactionCounts: Map<String, Int>,
    onCategoryClick: (String, String, Int, Int, Int) -> Unit,
    selectedMonth: Int,
    selectedYear: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .drawBehind {
                // Soft radial gradient glow in bottom-left corner
                val cornerRadius = 16.dp.toPx()
                val glowCenterX = -cornerRadius * 0.5f // Just outside the corner
                val glowCenterY = size.height + cornerRadius * 0.5f
                val glowRadius = 60.dp.toPx()
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentBlue.copy(alpha = 0.4f), // Bright blue at center
                            AppColors.AccentBlue.copy(alpha = 0.1f), // Fade to transparent
                            Color.Transparent
                        ),
                        center = Offset(glowCenterX, glowCenterY),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = Offset(glowCenterX, glowCenterY)
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.DarkCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categorySpending.forEach { spending ->
                val category = categoriesById[spending.categoryId]
                val budgetCategory = budgetCategories[spending.categoryId]
                val transactionCount = transactionCounts[spending.categoryId] ?: 0
                
                CategoryListItem(
                    categoryName = category?.name ?: "Unknown",
                    amount = spending.totalAmount,
                    transactionCount = transactionCount,
                    icon = category?.icon ?: 0,
                    iconColor = category?.color ?: "#007BFF",
                    budgetAmount = budgetCategory?.allocatedAmount ?: 0.0,
                    onItemClick = {
                        onCategoryClick(
                            spending.categoryId,
                            category?.name ?: "Unknown",
                            category?.icon ?: 0,
                            selectedMonth,
                            selectedYear
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No transactions found",
                style = MaterialTheme.typography.titleLarge,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Add some transactions to see your spending breakdown",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MonthPickerDialog(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentCalendar = Calendar.getInstance()
    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH)
    
    // Generate month-year options from 2023 to current
    val monthYearOptions = remember {
        val options = mutableListOf<Pair<Int, Int>>() // (year, month)
        for (year in 2023..currentYear) {
            val endMonth = if (year == currentYear) currentMonth else 11
            for (month in 0..endMonth) {
                options.add(year to month)
            }
        }
        options.reversed() // Most recent first
    }
    
    val monthNames = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    val selectedIndex = remember(selectedMonth, selectedYear) {
        monthYearOptions.indexOfFirst { it.first == selectedYear && it.second == selectedMonth }
            .takeIf { it >= 0 } ?: (monthYearOptions.size - 1)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.DarkCard
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select Month",
                    style = MaterialTheme.typography.headlineSmall,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(monthYearOptions.size) { index ->
                        val (year, month) = monthYearOptions[index]
                        val monthText = "${monthNames[month]} $year"
                        val isSelected = index == selectedIndex
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMonthSelected(month, year)
                                    onDismiss()
                                }
                                .padding(16.dp)
                                .background(
                                    if (isSelected) AppColors.AccentBlue.copy(alpha = 0.2f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) AppColors.AccentBlue else AppColors.TextPrimary
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = AppColors.AccentBlue,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}
