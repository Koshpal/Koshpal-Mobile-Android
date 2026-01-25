package com.koshpal_android.koshpalapp.ui.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

/**
 * Summary Card Component (Income or Expense)
 * Matches HTML design exactly:
 * - Rounded corners (16dp / rounded-xl)
 * - Icon + label row at top
 * - Amount below
 * - Green for income, default text for expense
 * - Border and shadow
 */
@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val formattedAmount = remember(amount) {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
    
    val colorScheme = MaterialTheme.colorScheme
    
    val iconColor = if (isIncome) {
        Color(0xFF1DB954) // Income green from HTML
    } else {
        Color(0xFFE53935) // Expense red from HTML
    }
    
    val amountColor = remember(isIncome, colorScheme) {
        if (isIncome) {
            Color(0xFF1DB954) // Income green
        } else {
            colorScheme.onSurface // Default text color
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon + Label Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.SouthWest else Icons.Default.NorthEast,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
            
            // Amount
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleLarge,
                color = amountColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

