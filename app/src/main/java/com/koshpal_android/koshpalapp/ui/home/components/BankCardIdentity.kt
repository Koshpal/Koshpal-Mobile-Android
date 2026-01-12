package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.ui.theme.KoshpalColors

@Composable
fun BankCardIdentity(
    bankName: String,
    accountNumber: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                // Light background tint based on theme
                if (MaterialTheme.colorScheme.surface == Color.White) {
                    Color(0xFFE0F2FE) // sky-200 equivalent
                } else {
                    KoshpalColors.Primary.copy(alpha = 0.2f) // primary/20
                }
            )
            .padding(16.dp), // p-4 equivalent
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        // Bank initials/icon in rounded square
        Box(
            modifier = Modifier
                .size(40.dp) // w-10 h-10
                .clip(RoundedCornerShape(8.dp)) // rounded-lg
                .background(
                    if (MaterialTheme.colorScheme.surface == Color.White) {
                        Color(0xFF0EA5E9) // sky-500
                    } else {
                        KoshpalColors.Primary
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bankName.take(2).uppercase(),
                color = Color.White,
                fontSize = 12.sp, // text-sm
                fontWeight = FontWeight.Bold
            )
        }

        // Bank name and account number
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = bankName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold, // font-bold
                    fontSize = 14.sp
                ),
                color = if (MaterialTheme.colorScheme.surface == Color.White) {
                    Color(0xFF111827) // slate-900
                } else {
                    Color.White // text-white
                },
                maxLines = 2
            )

            Text(
                text = "xx${accountNumber.takeLast(4)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp // text-[10px]
                ),
                color = if (MaterialTheme.colorScheme.surface == Color.White) {
                    Color(0xFF6B7280) // slate-500
                } else {
                    if (MaterialTheme.colorScheme.surface == Color.White) {
                        Color(0xFF7DD3FC) // sky-300
                    } else {
                        KoshpalColors.Primary.copy(alpha = 0.7f) // primary with opacity
                    }
                }
            )
        }
    }
}
