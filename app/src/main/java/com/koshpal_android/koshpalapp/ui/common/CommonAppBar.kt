package com.koshpal_android.koshpalapp.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koshpal_android.koshpalapp.ui.theme.AppColors
import java.util.Calendar

/**
 * Common App Bar for all fragments
 * Displays profile icon and time-based greeting
 */
@Composable
fun CommonAppBar(
    greeting: String = getGreetingText(),
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Icon with blue background and glow effect
        Box(
            modifier = Modifier
                .size(48.dp)
                .drawBehind {
                    // Blue glow effect around the circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AppColors.AccentBlue.copy(alpha = 0.3f),
                                AppColors.AccentBlue.copy(alpha = 0.1f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width / 2f + 4.dp.toPx()
                        ),
                        radius = size.width / 2f + 4.dp.toPx()
                    )
                }
                .clip(CircleShape)
                .background(AppColors.AccentBlue)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            // Blue user icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Time-based greeting text
        Text(
            text = greeting,
            color = AppColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Get greeting text based on time of day
 */
fun getGreetingText(): String {
    return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

