package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.koshpal_android.koshpalapp.ui.theme.KoshpalColors

@Composable
fun MoneyManagerBackground() {
    // Decorative background element - positioned at bottom-right
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 0.dp, bottom = 0.dp), // -right-10 -bottom-10 equivalent
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .size(160.dp) // w-40 h-40
                .clip(CircleShape)
                .background(
                    if (MaterialTheme.colorScheme.surface == Color.White) {
                        Color(0xFFDBEAFE) // blue-50
                    } else {
                        KoshpalColors.Primary.copy(alpha = 0.2f) // primary/20
                    }
                )
                .blur(radius = 48.dp) // blur-3xl equivalent
        )
    }
}
