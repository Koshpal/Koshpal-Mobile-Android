package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.ui.theme.KoshpalColors

@Composable
fun AddPaymentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value

    val shadowColor = if (MaterialTheme.colorScheme.surface == Color.White) {
        KoshpalColors.Primary.copy(alpha = 0.25f) // Light theme shadow
    } else {
        KoshpalColors.Primary.copy(alpha = 0.2f) // Dark theme shadow
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp) // h-14 equivalent
            .clip(RoundedCornerShape(12.dp)) // rounded-xl
            .background(KoshpalColors.Primary) // bg-primary
            .shadow(
                elevation = 16.dp, // shadow-lg
                shape = RoundedCornerShape(12.dp),
                spotColor = shadowColor
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Remove default ripple
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp) // py-4 equivalent
        ) {
            // Add icon
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White, // text-white
                modifier = Modifier.size(22.dp) // text-2xl equivalent
            )

            // Button text
            Text(
                text = "Add Payment",
                color = Color.White, // text-white
                fontSize = 14.sp, // Default size
                fontWeight = FontWeight.SemiBold, // font-semibold
                letterSpacing = 0.25.sp
            )
        }
    }
}
