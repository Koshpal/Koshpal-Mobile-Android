package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.model.TransactionType

@Composable
fun TransactionIcon(
    icon: ImageVector,
    transactionType: TransactionType,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (transactionType) {
        TransactionType.CREDIT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) // Green tint for income
        else -> MaterialTheme.colorScheme.surfaceVariant // Neutral for expenses
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when (transactionType) {
                TransactionType.CREDIT -> MaterialTheme.colorScheme.primary // Green for income
                else -> MaterialTheme.colorScheme.onSurfaceVariant // Neutral for expenses
            }
        )
    }
}
