package com.koshpal_android.koshpalapp.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LogoutRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    val pressedColor = Color(0xFFFFF1F1) // Light red background on press

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .background(if (isPressed) pressedColor else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFEF4444), // Red color
            modifier = Modifier.size(22.dp)
        )

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFFEF4444), // Red color
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )

        // Chevron
        androidx.compose.material3.Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFEF4444).copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
