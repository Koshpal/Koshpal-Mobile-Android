package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.model.BankSpending
import java.text.NumberFormat
import java.text.SimpleDateFormat

@Composable
fun BankCardDetails(
    bankSpending: BankSpending,
    currencyFormatter: NumberFormat,
    dateFormatter: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                if (MaterialTheme.colorScheme.surface == Color.White) {
                    Color.White // bg-white
                } else {
                    Color(0xFF374151) // card-dark equivalent
                }
            )
            .padding(16.dp), // p-4 equivalent
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        // Date and time (if available)
        bankSpending.lastUpdated?.let { timestamp ->
            Text(
                text = dateFormatter.format(java.util.Date(timestamp)),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp // text-[10px]
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant, // text-slate-400
                textAlign = TextAlign.End,
                modifier = Modifier.padding(bottom = 4.dp) // mb-1 equivalent
            )
        }

        // Amount (balance or last transaction amount)
        val amount = bankSpending.balance ?: bankSpending.totalSpending
        val isNegative = amount < 0

        Text(
            text = if (isNegative) {
                "-${currencyFormatter.format(Math.abs(amount)).replace("₹", "₹")}"
            } else {
                currencyFormatter.format(amount)
            },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, // font-bold
                fontSize = 24.sp // text-2xl
            ),
            color = if (MaterialTheme.colorScheme.surface == Color.White) {
                Color(0xFF111827) // slate-900
            } else {
                Color.White // text-white
            },
            textAlign = TextAlign.End
        )
    }
}
