package com.koshpal_android.koshpalapp.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.ui.account.components.KoshpalListRow

@Composable
fun AccountRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    isDestructive: Boolean = false
) {
    Column {
        KoshpalListRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            isDestructive = isDestructive
        )

        if (showDivider) {
            val dividerColor = if (MaterialTheme.colorScheme.surface == Color.White) {
                Color(0xFFE2E8F0).copy(alpha = 0.5f)
            } else {
                Color(0xFF23262F)
            }

            Divider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = dividerColor,
                thickness = 1.dp
            )
        }
    }
}
