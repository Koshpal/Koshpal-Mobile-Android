package com.koshpal_android.koshpalapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.R

/**
 * Bottom Navigation Bar - Material 3 compliant navigation container
 * Fixed to bottom with theme-aware background and subtle border
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

    val backgroundColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Standard bottom navigation height
            .drawBehind {
                // Theme-aware background
                drawRect(color = backgroundColor)

                // Subtle top border using outlineVariant
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(bottom = 8.dp), // Safe area padding
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        navigationItems.forEach { item ->
            BottomNavItem(
                item = item,
                isSelected = item.id == selectedItemId,
                onClick = { onItemSelected(item.id) }
            )
        }
    }
}
