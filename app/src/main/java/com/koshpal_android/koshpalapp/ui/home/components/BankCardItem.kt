package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.model.BankSpending
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BankCardItem(
    bankSpending: BankSpending,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = remember {
        SimpleDateFormat("hh:mm a • d, MMM", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    // Use real account number or fallback
    val accountNumber = remember(bankSpending.accountNumber, bankSpending.bankName) {
        bankSpending.accountNumber ?: run {
            val hash = bankSpending.bankName.hashCode()
            val last4 = Math.abs(hash) % 10000
            String.format("%04d", last4)
        }
    }

    // Debug: Simple visible card first
    Card(
        modifier = modifier
            .width(288.dp) // w-72 equivalent
            .height(176.dp) // h-44 equivalent
            .clip(RoundedCornerShape(12.dp)) // rounded-xl
            .clickable(onClick = onCardClick)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(12.dp)), // shadow-lg
        colors = CardDefaults.cardColors(
            containerColor = Color.Red // Make it obviously visible for debugging
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Yellow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${bankSpending.bankName}\n₹${bankSpending.totalSpending}",
                color = Color.Black,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
