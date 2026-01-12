package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.ui.theme.KoshpalColors

@Composable
fun MoneyManagerCard(
    monthLabel: String,
    spendAmount: String,
    incomeAmount: String,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)), // rounded-2xl
        colors = CardDefaults.cardColors(
            containerColor = if (MaterialTheme.colorScheme.surface == Color.White) {
                // Light theme: light surface with subtle primary tint
                MaterialTheme.colorScheme.surface
            } else {
                // Dark theme: primary with low opacity
                KoshpalColors.Primary.copy(alpha = 0.1f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // shadow-xl equivalent
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative background blur (UI-only)
            MoneyManagerBackground()

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp) // p-6 equivalent
            ) {
                // Header row
                MoneyManagerHeader(
                    monthLabel = monthLabel,
                    onMonthClick = onMonthClick
                )

                Spacer(modifier = Modifier.height(32.dp)) // mb-8 equivalent

                // Metrics grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp) // gap-8 equivalent
                ) {
                    // Spends column
                    MoneyMetric(
                        label = "Spends",
                        amount = spendAmount,
                        isSpends = true,
                        modifier = Modifier.weight(1f)
                    )

                    // Income column
                    MoneyMetric(
                        label = "Income",
                        amount = incomeAmount,
                        isSpends = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
