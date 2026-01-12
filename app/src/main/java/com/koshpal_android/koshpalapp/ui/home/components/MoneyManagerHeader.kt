package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.R
import com.koshpal_android.koshpalapp.ui.theme.KoshpalColors

@Composable
fun MoneyManagerHeader(
    monthLabel: String,
    onMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left section: Icon + Title + Chevron + Month
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp) // gap-2 equivalent
        ) {
            // Grid view icon
            Icon(
                painter = painterResource(id = R.drawable.ic_chart), // grid_view equivalent
                contentDescription = null,
                tint = KoshpalColors.Primary, // text-primary
                modifier = Modifier.size(20.dp)
            )

            // Title
            Text(
                text = "Money Manager",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold, // font-bold
                    fontSize = 14.sp // text-sm
                ),
                color = if (MaterialTheme.colorScheme.surface == androidx.compose.ui.graphics.Color.White) {
                    androidx.compose.ui.graphics.Color(0xFF1F2937) // slate-800
                } else {
                    androidx.compose.ui.graphics.Color(0xFFF1F5F9) // slate-100
                }
            )

            // Small chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), // text-slate-400
                modifier = Modifier.size(12.dp) // text-xs
            )

            // Month label
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold, // font-bold
                    fontSize = 14.sp // text-sm
                ),
                color = KoshpalColors.Primary // text-primary
            )
        }

        // Right section: Month selector button
        Box(
            modifier = Modifier
                .size(32.dp) // w-8 h-8
                .clip(CircleShape)
                .background(
                    if (MaterialTheme.colorScheme.surface == androidx.compose.ui.graphics.Color.White) {
                        androidx.compose.ui.graphics.Color(0xFFF9FAFB) // slate-50
                    } else {
                        androidx.compose.ui.graphics.Color(0xFF1E293B) // slate-800
                    }
                )
                .clickable(onClick = onMonthClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Change month",
                tint = if (MaterialTheme.colorScheme.surface == androidx.compose.ui.graphics.Color.White) {
                    androidx.compose.ui.graphics.Color(0xFF6B7280) // slate-600
                } else {
                    androidx.compose.ui.graphics.Color(0xFFCBD5E1) // slate-300
                },
                modifier = Modifier.size(14.dp) // text-sm
            )
        }
    }
}
