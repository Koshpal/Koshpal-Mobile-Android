package com.koshpal_android.koshpalapp.ui.categories.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class for category budget item
 */
data class CategoryBudgetItem(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: Int,
    val categoryColor: String,
    val currentSpending: Double,
    var budgetAmount: Double
)

/**
 * Main Set Monthly Budget Screen Composable
 * Stateless design - all data and handlers passed as parameters
 */
@Composable
fun SetMonthlyBudgetScreen(
    totalBudget: Double,
    categoryBudgets: List<CategoryBudgetItem>,
    monthDisplay: String, // e.g., "Nov 2025"
    onBackClicked: () -> Unit,
    onMonthClick: () -> Unit,
    onTotalBudgetChanged: (Double) -> Unit,
    onCategoryBudgetChanged: (String, Double) -> Unit, // categoryId, newAmount
    onAddCategoryClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.PureBlack)
    ) {
        // Top App Bar
        TopAppBar(
            onBackClicked = onBackClicked,
            monthDisplay = monthDisplay,
            onMonthClick = onMonthClick
        )
        
        // Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Total Monthly Budget Card
            item {
                TotalBudgetCard(
                    totalBudget = totalBudget,
                    onTotalBudgetChanged = onTotalBudgetChanged
                )
            }
            
            // Set Budget by Category Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set Budget by Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // ADD CATEGORY Button (Send button style)
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(AppColors.DarkButtonBg)
                            .clickable(onClick = onAddCategoryClicked)
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
                                contentDescription = "Add Category",
                                tint = AppColors.AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ADD CATEGORY",
                                color = AppColors.AccentBlue,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Category List Card
            item {
                CategoryListCard(
                    categoryBudgets = categoryBudgets,
                    onCategoryBudgetChanged = onCategoryBudgetChanged
                )
            }
            
            // Bottom spacing for Save Button
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Save Button (Fixed at bottom)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SaveBudgetButton(
            onClick = onSaveClicked,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Top App Bar with back button, title, and month picker
 */
@Composable
private fun TopAppBar(
    onBackClicked: () -> Unit,
    monthDisplay: String,
    onMonthClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = AppColors.TextPrimary
            )
        }
        
        // Title
        Text(
            text = "Set Monthly Budget",
            style = MaterialTheme.typography.titleLarge,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        // Month Picker
        Row(
            modifier = Modifier
                .clickable(onClick = onMonthClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextPrimary
            )
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Month",
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Total Monthly Budget Card with corner glow VFX
 */
@Composable
private fun TotalBudgetCard(
    totalBudget: Double,
    onTotalBudgetChanged: (Double) -> Unit
) {
    var budgetText by remember { mutableStateOf(String.format("%.0f", totalBudget)) }
    
    // Update text when totalBudget changes externally
    LaunchedEffect(totalBudget) {
        budgetText = String.format("%.0f", totalBudget)
    }
    
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
                val glowCenterX = -cornerRadius * 0.5f
                val glowCenterY = size.height + cornerRadius * 0.5f
                val glowRadius = 60.dp.toPx()
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentBlue.copy(alpha = 0.4f),
                            AppColors.AccentBlue.copy(alpha = 0.1f),
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
                .padding(24.dp)
        ) {
            Text(
                text = "Total Monthly Budget",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Budget Input Field
            OutlinedTextField(
                value = budgetText,
                onValueChange = { newValue ->
                    budgetText = newValue
                    val amount = newValue.toDoubleOrNull() ?: 0.0
                    onTotalBudgetChanged(amount)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = AppColors.TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary,
                    focusedBorderColor = AppColors.AccentBlue,
                    unfocusedBorderColor = AppColors.TextTertiary,
                    focusedLabelColor = AppColors.TextSecondary,
                    unfocusedLabelColor = AppColors.TextSecondary,
                    cursorColor = AppColors.AccentBlue
                ),
                leadingIcon = {
                    Text(
                        text = "₹",
                        color = AppColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * Category List Card with corner glow VFX
 */
@Composable
private fun CategoryListCard(
    categoryBudgets: List<CategoryBudgetItem>,
    onCategoryBudgetChanged: (String, Double) -> Unit
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
                val glowCenterX = -cornerRadius * 0.5f
                val glowCenterY = size.height + cornerRadius * 0.5f
                val glowRadius = 60.dp.toPx()
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentBlue.copy(alpha = 0.4f),
                            AppColors.AccentBlue.copy(alpha = 0.1f),
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
            categoryBudgets.forEach { category ->
                CategoryBudgetItemRow(
                    category = category,
                    onBudgetChanged = { newAmount ->
                        onCategoryBudgetChanged(category.categoryId, newAmount)
                    }
                )
            }
        }
    }
}

/**
 * Individual Category Budget Item Row
 */
@Composable
private fun CategoryBudgetItemRow(
    category: CategoryBudgetItem,
    onBudgetChanged: (Double) -> Unit
) {
    var budgetText by remember(category.budgetAmount) {
        mutableStateOf(
            if (category.budgetAmount > 0) {
                String.format("%.0f", category.budgetAmount)
            } else {
                ""
            }
        )
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Icon with colored background
        val categoryColor = try {
            Color(android.graphics.Color.parseColor(category.categoryColor))
        } catch (e: Exception) {
            AppColors.AccentBlue
        }
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(categoryColor),
            contentAlignment = Alignment.Center
        ) {
            if (category.categoryIcon != 0) {
                Image(
                    painter = painterResource(id = category.categoryIcon),
                    contentDescription = category.categoryName,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(AppColors.IconPrimary),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Fallback: colored circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.3f))
                )
            }
        }
        
        // Category Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.categoryName,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Current spending: ₹${String.format("%.0f", category.currentSpending)}",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }
        
        // Budget Input Field
        Row(
            modifier = Modifier
                .background(
                    color = AppColors.CategoryIconBg,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₹",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            
            BasicTextField(
                value = budgetText,
                onValueChange = { newValue ->
                    budgetText = newValue
                    val amount = newValue.toDoubleOrNull() ?: 0.0
                    onBudgetChanged(amount)
                },
                modifier = Modifier.width(80.dp),
                textStyle = TextStyle(
                    color = AppColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (budgetText.isEmpty()) {
                            Text(
                                text = "0",
                                color = AppColors.TextSecondary,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

/**
 * Save Budget Button with strong blue glow effect (Get started button style)
 */
@Composable
private fun SaveBudgetButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(AppColors.DarkButtonBg)
            .clickable(onClick = onClick)
            .drawBehind {
                // Strong, soft blue glow emanating from the entire perimeter
                val centerX = size.width / 2
                val centerY = size.height / 2
                val maxDimension = size.maxDimension
                
                // Create multiple concentric glow layers for depth
                // Layer 1: Large outer glow (softest, spreads furthest)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentBlue.copy(alpha = 0.5f),
                            AppColors.AccentBlue.copy(alpha = 0.3f),
                            AppColors.AccentBlue.copy(alpha = 0.15f),
                            AppColors.AccentBlue.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = maxDimension * 1.2f
                    ),
                    radius = maxDimension * 1.2f,
                    center = Offset(centerX, centerY)
                )
                
                // Layer 2: Medium glow (moderate intensity)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentBlue.copy(alpha = 0.6f),
                            AppColors.AccentBlue.copy(alpha = 0.4f),
                            AppColors.AccentBlue.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = maxDimension * 0.9f
                    ),
                    radius = maxDimension * 0.9f,
                    center = Offset(centerX, centerY)
                )
                
                // Layer 3: Inner glow (brightest, closest to edge)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppColors.AccentBlue.copy(alpha = 0.7f),
                            AppColors.AccentBlue.copy(alpha = 0.5f),
                            AppColors.AccentBlue.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = maxDimension * 0.7f
                    ),
                    radius = maxDimension * 0.7f,
                    center = Offset(centerX, centerY)
                )
            }
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = AppColors.AccentBlue.copy(alpha = 0.7f),
                ambientColor = AppColors.AccentBlue.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SAVE BUDGET",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 16.sp
        )
    }
}

