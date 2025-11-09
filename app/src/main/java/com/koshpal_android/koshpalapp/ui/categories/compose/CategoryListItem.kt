package com.koshpal_android.koshpalapp.ui.categories.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import com.koshpal_android.koshpalapp.ui.theme.Typography

/**
 * Category List Item Composable
 * Displays a single category with colored icon, name, amount, transaction count, and gradient progress bar
 */
@Composable
fun CategoryListItem(
    categoryName: String,
    amount: Double,
    transactionCount: Int,
    icon: Int,
    iconColor: String,
    budgetAmount: Double,
    onItemClick: () -> Unit
) {
    val hasBudget = budgetAmount > 0
    val percentage = if (hasBudget) {
        ((amount / budgetAmount) * 100).toInt().coerceIn(0, 100)
    } else {
        0
    }
    
    // Parse icon color for background
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(iconColor))
    } catch (e: Exception) {
        AppColors.AccentBlue
    }
    
    // Gradient for progress bar
    val progressGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF007BFF), // Bright blue
            Color(0xFF0056B3)  // Darker blue
        )
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Icon with original colored background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(categoryColor), // Use original category color
            contentAlignment = Alignment.Center
        ) {
            if (icon != 0) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = categoryName,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(Color.White) // White icon on colored background
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
            // Category Name and Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryName,
                    style = Typography.titleMedium,
                    color = AppColors.TextPrimary, // White for category titles
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "₹${String.format("%,.0f", amount)}",
                    style = Typography.titleMedium,
                    color = AppColors.TextPrimary, // White for main amounts
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Transaction Count
            Text(
                text = if (transactionCount == 1) "1 transaction" else "$transactionCount transactions",
                style = Typography.bodySmall,
                color = AppColors.TextSecondary // Light gray for descriptive text
            )
            
            // Progress Bar (if budget is set) with gradient
            if (hasBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Custom gradient progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AppColors.ProgressBarBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage / 100f)
                                .fillMaxHeight()
                                .background(
                                    brush = progressGradient,
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    
                    Text(
                        text = "$percentage% of ₹${String.format("%,.0f", budgetAmount)}",
                        style = Typography.bodySmall,
                        color = AppColors.TextSecondary // Light gray for budget info
                    )
                }
            }
        }
    }
}
