package com.koshpal_android.koshpalapp.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.R

/**
 * Bottom Navigation Bar - Material 3 compliant navigation container
 * Premium fintech-grade UI with theme-aware styling
 * 
 * Design:
 * - Surface background (theme-aware)
 * - Subtle top divider using outlineVariant
 * - Clear active tab indication
 * - 48dp minimum touch targets
 */
@Composable
fun BottomNavBar(
    selectedItemId: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navigationItems = listOf(
        NavigationItemData(R.id.home, R.drawable.ic_home, "Home"),
        NavigationItemData(R.id.transactions, R.drawable.ic_rup, "Payments"),
        NavigationItemData(R.id.categories, R.drawable.ic_categ, "Categories"),
        NavigationItemData(R.id.reminders, R.drawable.ic_notifications, "Reminders")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp) // Standard bottom navigation height
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationItems.forEach { item ->
            BottomNavItem(
                item = item,
                isSelected = item.id == selectedItemId,
                onClick = { onItemSelected(item.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
