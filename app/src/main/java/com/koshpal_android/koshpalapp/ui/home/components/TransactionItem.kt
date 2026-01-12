package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.model.Transaction
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    // Get icon based on category (simplified - using star as default)
    val icon: ImageVector = Icons.Default.Star

    // Format date and time
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val formattedDateTime = remember(transaction.date) {
        dateFormat.format(Date(transaction.date))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            TransactionIcon(
                icon = icon,
                transactionType = transaction.type,
                modifier = Modifier.size(40.dp)
            )

            // Text content (title and meta)
            TransactionText(
                title = transaction.merchant.ifEmpty { transaction.description },
                dateTime = formattedDateTime,
                transactionType = transaction.type,
                modifier = Modifier.weight(1f)
            )

            // Amount and action
            TransactionAmount(
                amount = transaction.amount,
                transactionType = transaction.type,
                currencyFormatter = currencyFormatter
            )
        }
    }
}
