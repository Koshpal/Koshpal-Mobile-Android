package com.koshpal_android.koshpalapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Bottom Navigation Active Indicator
 * Small circular dot that appears below active navigation items
 */
@Composable
fun BottomNavIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(4.dp) // Small indicator dot
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary) // Primary color for active state
    )
}
