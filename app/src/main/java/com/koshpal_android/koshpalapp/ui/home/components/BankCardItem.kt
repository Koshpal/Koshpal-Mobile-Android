package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

    Card(
        modifier = modifier
            .width(288.dp) // w-72 equivalent
            .height(176.dp) // h-44 equivalent
            .clip(RoundedCornerShape(12.dp)) // rounded-xl
            .clickable(onClick = onCardClick)
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(12.dp)), // shadow-lg
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Transparent to let sections show their backgrounds
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left section - Identity strip (1/3 width)
            BankCardIdentity(
                bankName = bankSpending.bankName,
                accountNumber = accountNumber,
                modifier = Modifier.weight(1f)
            )

            // Right section - Details (2/3 width)
            BankCardDetails(
                bankSpending = bankSpending,
                currencyFormatter = currencyFormatter,
                dateFormatter = dateFormatter,
                modifier = Modifier.weight(2f)
            )
        }
    }
}
