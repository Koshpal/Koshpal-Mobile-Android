package com.koshpal_android.koshpalapp.ui.budget.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

/**
 * Budget Amount Text Component
 * Formats and displays budget amounts with proper styling
 * 
 * Design:
 * - Large, bold text for amounts
 * - Theme-aware colors
 * - Currency formatting
 */
@Composable
fun BudgetAmountText(
    amount: Double,
    style: AmountStyle = AmountStyle.Large,
    modifier: Modifier = Modifier
) {
    val formattedAmount = remember(amount) {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
    
    val fontSize = when (style) {
        AmountStyle.Large -> 24.sp
        AmountStyle.Medium -> 20.sp
        AmountStyle.Small -> 16.sp
    }
    
    Text(
        text = formattedAmount,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

enum class AmountStyle {
    Large,
    Medium,
    Small
}

