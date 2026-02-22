package com.koshpal_android.koshpalapp.ui.transactions.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
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
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

/**
 * Amount Text Component
 * Matches HTML design exactly:
 * - Amount with prefix (+ or -)
 * - Green for income, default text for expense
 * - Direction icon (south_west for income, north_east for expense)
 * - Right-aligned
 */
@Composable
fun AmountText(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val formattedAmount = remember(transaction.amount) {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(transaction.amount)
    }
    
    val (amountColor, prefix, directionIcon) = remember(transaction.type, colorScheme) {
        when (transaction.type) {
            TransactionType.CREDIT -> {
                Triple(
                    Color(0xFF1DB954), // Income green
                    "+",
                    Icons.Default.SouthWest
                )
            }
            TransactionType.DEBIT -> {
                Triple(
                    colorScheme.onSurface, // Default text
                    "-",
                    Icons.Default.NorthEast
                )
            }
            TransactionType.TRANSFER -> {
                Triple(
                    colorScheme.onSurface,
                    "",
                    Icons.Default.NorthEast
                )
            }
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$prefix$formattedAmount",
            style = MaterialTheme.typography.bodyLarge,
            color = amountColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = directionIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

