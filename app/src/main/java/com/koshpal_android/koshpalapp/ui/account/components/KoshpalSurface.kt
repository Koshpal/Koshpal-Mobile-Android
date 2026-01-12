package com.koshpal_android.koshpalapp.ui.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KoshpalSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaceColor = if (MaterialTheme.colorScheme.surface == Color.White) {
        Color(0xFFF8FAFC) // Light theme surface from HTML
    } else {
        Color(0xFF181A20) // Dark theme surface from HTML
    }

    val borderColor = if (MaterialTheme.colorScheme.surface == Color.White) {
        Color(0xFFE2E8F0).copy(alpha = 0.5f) // Light theme border
    } else {
        Color(0xFF23262F) // Dark theme border
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}
