package com.koshpal_android.koshpalapp.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Profile avatar component - circular container with user icon
 * Theme-aware background with proper touch target
 */
@Composable
fun ProfileAvatar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp) // w-12 h-12 (48px)
            .clip(CircleShape)
            .background(
                // Theme-aware background
                if (MaterialTheme.colorScheme.surface == Color.White) {
                    // Light theme: subtle primary tint
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else {
                    // Dark theme: use surface variant
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = MaterialTheme.colorScheme.primary, // Use primary color for icon
            modifier = Modifier.size(24.dp) // text-xl
        )
    }
}
