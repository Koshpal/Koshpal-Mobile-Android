package com.koshpal_android.koshpalapp.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Individual Bottom Navigation Item
 * Premium Material 3 design with clear active/inactive states
 * 
 * Design:
 * - Active: Primary color icon + indicator dot
 * - Inactive: Muted icon color
 * - Minimum 48dp touch target
 */
@Composable
fun BottomNavItem(
    item: NavigationItemData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // Minimum 48dp touch target
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with theme-aware coloring
        Icon(
            painter = painterResource(id = item.iconResId),
            contentDescription = item.title,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary // Active: primary color
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant // Inactive: muted
            },
            modifier = Modifier.size(24.dp)
        )

        // Active indicator (small dot below icon)
        Spacer(modifier = Modifier.height(4.dp))
        if (isSelected) {
            BottomNavIndicator()
        } else {
            Spacer(modifier = Modifier.height(4.dp)) // Maintain consistent spacing
        }
    }
}
