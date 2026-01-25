package com.koshpal_android.koshpalapp.ui.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionCategory
import com.koshpal_android.koshpalapp.model.TransactionType

/**
 * Transaction Icon Component
 * Matches HTML design exactly:
 * - Circular icon container (w-12 h-12 = 48dp)
 * - Colored background based on transaction type
 * - White icon inside
 * - Icons: attach_money, trending_up, account_balance, storefront, person_outline
 */
@Composable
fun TransactionIcon(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val category = remember(transaction.categoryId) {
        TransactionCategory.getDefaultCategories().find { it.id == transaction.categoryId }
            ?: TransactionCategory.getDefaultCategories().find { it.id == "others" }
    }
    
    val (icon, backgroundColor, iconColor) = remember(transaction.type, category, colorScheme) {
        when (transaction.type) {
            TransactionType.CREDIT -> {
                // Income - green background
                val bgColor = Color(0xFF1DB954).copy(alpha = 0.1f)
                val iconTint = Color(0xFF1DB954)
                // Use category-specific icon or default
                val iconVector = when {
                    category?.name?.lowercase()?.contains("money") == true -> Icons.Default.AttachMoney
                    category?.name?.lowercase()?.contains("store") == true -> Icons.Default.Storefront
                    category?.name?.lowercase()?.contains("person") == true -> Icons.Default.PersonOutline
                    else -> Icons.Default.AttachMoney
                }
                Triple(iconVector, bgColor, iconTint)
            }
            TransactionType.DEBIT -> {
                // Expense - blue/neutral background
                val bgColor = colorScheme.primary.copy(alpha = 0.1f)
                val iconTint = colorScheme.primary
                val iconVector = when {
                    category?.name?.lowercase()?.contains("trend") == true -> Icons.Default.TrendingUp
                    category?.name?.lowercase()?.contains("bank") == true -> Icons.Default.AccountBalance
                    else -> Icons.Default.TrendingUp
                }
                Triple(iconVector, bgColor, iconTint)
            }
            TransactionType.TRANSFER -> {
                // Transfer - neutral background
                val bgColor = colorScheme.surfaceVariant
                val iconTint = colorScheme.onSurfaceVariant
                Triple(Icons.Default.AccountBalance, bgColor, iconTint)
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(48.dp)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = transaction.merchant.ifEmpty { transaction.description },
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

