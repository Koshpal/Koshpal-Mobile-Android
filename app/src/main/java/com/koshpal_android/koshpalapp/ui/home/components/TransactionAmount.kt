package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransactionAmount(
    amount: Double,
    transactionType: TransactionType,
    currencyFormatter: NumberFormat,
    modifier: Modifier = Modifier
) {
    val amountColor = when (transactionType) {
        TransactionType.CREDIT -> Color(0xFF22C55E) // Green for income
        else -> MaterialTheme.colorScheme.onSurface // Neutral for expenses
    }

    val prefix = when (transactionType) {
        TransactionType.CREDIT -> "+"
        else -> "-"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$prefix${currencyFormatter.format(amount)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            color = amountColor
        )

        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
