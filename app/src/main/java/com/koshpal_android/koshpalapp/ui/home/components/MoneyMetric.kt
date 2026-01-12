package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoneyMetric(
    label: String,
    amount: String,
    isSpends: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp) // mb-1 equivalent
    ) {
        // Icon + Label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // gap-2 equivalent
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(20.dp) // w-5 h-5
                    .clip(CircleShape)
                    .background(
                        if (isSpends) {
                            // Spends: red tint
                            if (MaterialTheme.colorScheme.surface == Color.White) {
                                Color(0xFFFEE2E2) // red-100
                            } else {
                                Color(0xFFEF4444).copy(alpha = 0.2f) // red-500/20
                            }
                        } else {
                            // Income: green tint
                            if (MaterialTheme.colorScheme.surface == Color.White) {
                                Color(0xFFF0FDF4) // green-100
                            } else {
                                Color(0xFF22C55E).copy(alpha = 0.2f) // green-500/20
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSpends) Icons.Default.NorthEast else Icons.Default.SouthWest,
                    contentDescription = null,
                    tint = if (isSpends) {
                        // Red for spends
                        if (MaterialTheme.colorScheme.surface == Color.White) {
                            Color(0xFFDC2626) // red-600
                        } else {
                            Color(0xFFF87171) // red-400
                        }
                    } else {
                        // Green for income
                        if (MaterialTheme.colorScheme.surface == Color.White) {
                            Color(0xFF16A34A) // green-600
                        } else {
                            Color(0xFF4ADE80) // green-400
                        }
                    },
                    modifier = Modifier.size(12.dp) // text-xs
                )
            }

            // Label
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold, // font-bold
                    fontSize = 10.sp, // text-[10px]
                    letterSpacing = 0.1.sp // tracking-widest equivalent
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant // text-slate-500 dark:text-slate-400
            )
        }

        // Amount
        Text(
            text = amount,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold, // font-bold
                fontSize = 24.sp // text-2xl
            ),
            color = if (MaterialTheme.colorScheme.surface == Color.White) {
                Color(0xFF111827) // slate-900
            } else {
                Color.White // text-white
            }
        )
    }
}
