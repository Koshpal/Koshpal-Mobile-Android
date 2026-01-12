package com.koshpal_android.koshpalapp.ui.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KoshpalListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    val pressedColor = if (MaterialTheme.colorScheme.surface == Color.White) {
        Color(0xFFF1F5F9) // Light theme pressed state
    } else {
        Color.White.copy(alpha = 0.02f) // Dark theme pressed state
    }

    val destructiveColor = if (isDestructive) Color(0xFFFFF1F1) else pressedColor

    val iconColor = if (isDestructive) {
        Color(0xFFEF4444) // Red for destructive actions
    } else {
        Color(0xFF0052FF) // Koshpal blue
    }

    val textColor = if (isDestructive) {
        Color(0xFFEF4444) // Red for destructive actions
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Remove default ripple
                onClick = onClick
            )
            .background(if (isPressed) destructiveColor else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )

        // Title and subtitle
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = textColor
            )

            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isDestructive) Color(0xFFEF4444).copy(alpha = 0.8f) else Color(0xFFF59E0B), // Amber for status
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Chevron
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDestructive) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFCBD5E1),
            modifier = Modifier.size(20.dp)
        )
    }
}
